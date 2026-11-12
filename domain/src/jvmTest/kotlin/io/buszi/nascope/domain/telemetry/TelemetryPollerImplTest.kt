package io.buszi.nascope.domain.telemetry

import app.cash.turbine.turbineScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.buszi.nascope.domain.foreground.AppForegroundState
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryPollerImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val source = mockk<TelemetrySource>()
    private val sources = MutableStateFlow<TelemetrySource?>(source)
    private val sourceProvider = mockk<TelemetrySourceProvider> { every { this@mockk.source } returns sources }
    private val foreground = MutableStateFlow(true)
    private val appForegroundState = mockk<AppForegroundState> { every { isForeground } returns foreground }
    private val interval = MutableStateFlow(RefreshInterval.S2)
    private val observeRefreshInterval = mockk<ObserveRefreshIntervalUseCase> { every { this@mockk() } returns interval }
    private val applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.dispatcher)

    private val poller by lazy {
        TelemetryPollerImpl(
            sourceProvider = sourceProvider,
            appForegroundState = appForegroundState,
            observeRefreshInterval = observeRefreshInterval,
            applicationScope = applicationScope,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    private fun TestScope.step(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun cpuFetches(count: Int) = coVerify(exactly = count) { source.fetchCpu() }

    @Test
    fun pollerFetchesImmediatelyAndEmitsLoadingThenTheValue() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()

            assertEquals(listOf(Polled.Loading, Polled.Value(cpu, isStale = false)), turbine.cancelAndConsumeItems())
            cpuFetches(1)
        }
    }

    @Test
    fun pollerPollsFastPluginsAtTheRefreshInterval() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            step(1_999)
            cpuFetches(1)
            step(1)
            cpuFetches(2)
            step(2_000)
            cpuFetches(3)

            turbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun pollerPollsSlowPluginsAtFiveTimesTheIntervalCappedAtThirtySeconds() = runTest {
        turbineScope {
            coEvery { source.fetchUptime() } returns Ok(60.seconds)

            val turbine = poller.uptime.testIn(this)
            runCurrent()
            step(9_999)
            coVerify(exactly = 1) { source.fetchUptime() }
            step(1)
            coVerify(exactly = 2) { source.fetchUptime() }

            interval.value = RefreshInterval.S10
            step(10_000) // the wait that started before the change still lasts 10 s
            coVerify(exactly = 3) { source.fetchUptime() }
            step(29_999)
            coVerify(exactly = 3) { source.fetchUptime() }
            step(1)
            coVerify(exactly = 4) { source.fetchUptime() }

            turbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun pollerFetchesStaticPluginsOnceAndRetriesAtTheSlowPeriodUntilTheyLoad() = runTest {
        turbineScope {
            coEvery { source.fetchSystem() } returnsMany listOf(
                Err(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
                Err(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
                Ok(system),
            )

            val turbine = poller.system.testIn(this)
            runCurrent()
            step(10_000)
            step(10_000)
            coVerify(exactly = 3) { source.fetchSystem() }
            step(600_000)
            coVerify(exactly = 3) { source.fetchSystem() }

            assertEquals(
                listOf(
                    Polled.Loading,
                    Polled.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
                    Polled.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
                    Polled.Value(system, isStale = false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun pollerAppliesAChangedIntervalFromTheNextWait() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            step(2_000)
            interval.value = RefreshInterval.S10
            step(2_000) // the running wait was started with 2 s
            cpuFetches(3)
            step(9_999)
            cpuFetches(3)
            step(1)
            cpuFetches(4)

            turbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun pollerKeepsTheValueStaleForTwoFailuresThenFailsAndRecovers() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returnsMany listOf(
                Ok(cpu),
                Err(offline),
                Err(offline),
                Err(offline),
                Err(offline),
                Ok(cpu),
            )

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            repeat(5) { step(2_000) }

            assertEquals(
                listOf(
                    Polled.Loading,
                    Polled.Value(cpu, isStale = false),
                    Polled.Value(cpu, isStale = true),
                    Polled.Value(cpu, isStale = true),
                    Polled.Failed(offline),
                    Polled.Failed(offline),
                    Polled.Value(cpu, isStale = false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun pollerFailsAtOnceWhenTheFirstFetchFails() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Err(offline)

            val turbine = poller.cpu.testIn(this)
            runCurrent()

            assertEquals(listOf(Polled.Loading, Polled.Failed(offline)), turbine.cancelAndConsumeItems())
        }
    }

    @Test
    fun pollerFailsAtOnceForAnUnavailablePluginAndSlowsItDown() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returnsMany listOf(
                Ok(cpu),
                Err(TelemetryFailure.PluginUnavailable),
                Ok(cpu),
            )

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            step(2_000)
            step(9_999)
            cpuFetches(2)
            step(1)
            cpuFetches(3)

            assertEquals(
                listOf(
                    Polled.Loading,
                    Polled.Value(cpu, isStale = false),
                    Polled.Failed(TelemetryFailure.PluginUnavailable),
                    Polled.Value(cpu, isStale = false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun pollerRestartsFromLoadingOnANewSourceAndNeverEmitsTheOldValueAgain() = runTest {
        turbineScope {
            val otherSource = mockk<TelemetrySource>()
            coEvery { source.fetchCpu() } returns Ok(cpu)
            coEvery { otherSource.fetchCpu() } returns Ok(otherCpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            sources.value = otherSource
            runCurrent()

            assertEquals(
                listOf(
                    Polled.Loading,
                    Polled.Value(cpu, isStale = false),
                    Polled.Loading,
                    Polled.Value(otherCpu, isStale = false),
                ),
                turbine.cancelAndConsumeItems(),
            )
            cpuFetches(1)
        }
    }

    @Test
    fun pollerPollsNothingWithoutASource() = runTest {
        turbineScope {
            sources.value = null

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            step(60_000)

            assertEquals(listOf(Polled.Loading), turbine.cancelAndConsumeItems())
            cpuFetches(0)
        }
    }

    @Test
    fun pollerMakesNoRequestInTheBackgroundAndContinuesAfterAShortPause() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            foreground.value = false
            runCurrent()
            step(29_999)
            cpuFetches(1)

            foreground.value = true
            runCurrent()

            cpuFetches(2)
            assertEquals(
                listOf(Polled.Loading, Polled.Value(cpu, isStale = false), Polled.Value(cpu, isStale = false)),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun pollerRestartsFromLoadingAfterAPauseOverThirtySeconds() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returnsMany listOf(Ok(cpu), Ok(otherCpu))

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            foreground.value = false
            runCurrent()
            step(30_000)
            foreground.value = true
            runCurrent()

            assertEquals(
                listOf(
                    Polled.Loading,
                    Polled.Value(cpu, isStale = false),
                    Polled.Loading,
                    Polled.Value(otherCpu, isStale = false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun pollerRefreshPollsEveryRunningLoopImmediately() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)
            coEvery { source.fetchUptime() } returns Ok(60.seconds)

            val cpuTurbine = poller.cpu.testIn(this)
            val uptimeTurbine = poller.uptime.testIn(this)
            runCurrent()
            step(500)
            poller.refresh()
            runCurrent()

            cpuFetches(2)
            coVerify(exactly = 2) { source.fetchUptime() }
            cpuTurbine.cancelAndConsumeItems()
            uptimeTurbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun pollerSharesOneLoopBetweenCollectorsAndNeverRequestsUnobservedPlugins() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val first = poller.cpu.testIn(this)
            val second = poller.cpu.testIn(this)
            runCurrent()
            step(2_000)

            cpuFetches(2)
            coVerify(exactly = 0) { source.fetchLoad() }
            assertEquals(first.cancelAndConsumeItems(), second.cancelAndConsumeItems())
        }
    }

    @Test
    fun pollerStopsTheLoopWhenNobodyCollects() = runTest {
        turbineScope {
            coEvery { source.fetchCpu() } returns Ok(cpu)

            val turbine = poller.cpu.testIn(this)
            runCurrent()
            turbine.cancelAndConsumeItems()
            step(60_000)

            cpuFetches(1)
        }
    }

    @Test
    fun pollerMapsEveryPropertyToItsSourceFunction() = runTest {
        turbineScope {
            val unavailable = Err(TelemetryFailure.PluginUnavailable)
            coEvery { source.fetchSystem() } returns unavailable
            coEvery { source.fetchUptime() } returns unavailable
            coEvery { source.fetchLoad() } returns unavailable
            coEvery { source.fetchCpu() } returns unavailable
            coEvery { source.fetchCpuCores() } returns unavailable
            coEvery { source.fetchCpuInfo() } returns unavailable
            coEvery { source.fetchMemory() } returns unavailable
            coEvery { source.fetchSwap() } returns unavailable
            coEvery { source.fetchFileSystems() } returns unavailable
            coEvery { source.fetchDiskIo() } returns unavailable
            coEvery { source.fetchNetworkInterfaces() } returns unavailable
            coEvery { source.fetchSensors() } returns unavailable
            coEvery { source.fetchContainers() } returns unavailable
            coEvery { source.fetchProcessCount() } returns unavailable
            coEvery { source.fetchProcesses() } returns unavailable

            val flows: List<Flow<Polled<*>>> = listOf(
                poller.system, poller.uptime, poller.load, poller.cpu, poller.cpuCores, poller.cpuInfo,
                poller.memory, poller.swap, poller.fileSystems, poller.diskIo, poller.networkInterfaces,
                poller.sensors, poller.containers, poller.processCount, poller.processes,
            )
            val turbines = flows.map { it.testIn(this) }
            runCurrent()

            coVerify(exactly = 1) { source.fetchSystem() }
            coVerify(exactly = 1) { source.fetchUptime() }
            coVerify(exactly = 1) { source.fetchLoad() }
            coVerify(exactly = 1) { source.fetchCpu() }
            coVerify(exactly = 1) { source.fetchCpuCores() }
            coVerify(exactly = 1) { source.fetchCpuInfo() }
            coVerify(exactly = 1) { source.fetchMemory() }
            coVerify(exactly = 1) { source.fetchSwap() }
            coVerify(exactly = 1) { source.fetchFileSystems() }
            coVerify(exactly = 1) { source.fetchDiskIo() }
            coVerify(exactly = 1) { source.fetchNetworkInterfaces() }
            coVerify(exactly = 1) { source.fetchSensors() }
            coVerify(exactly = 1) { source.fetchContainers() }
            coVerify(exactly = 1) { source.fetchProcessCount() }
            coVerify(exactly = 1) { source.fetchProcesses() }
            turbines.forEach { it.cancelAndConsumeItems() }
        }
    }

    companion object {
        private val cpu = CpuUsage(10f, 5f, 3f, 1f, 90f, 4)
        private val otherCpu = CpuUsage(80f, 60f, 20f, 0f, 20f, 4)
        private val system = SystemInfo("nas", "Linux", "6.1", "Debian", "x86_64", "NAS")
        private val offline = TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused)
    }
}
