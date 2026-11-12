package io.buszi.nascope.domain.telemetry.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class ObservingUseCasesTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val poller = mockk<TelemetryPoller>()

    private val system = MutableStateFlow<Polled<SystemInfo>>(Polled.Loading)
    private val uptime = MutableStateFlow<Polled<kotlin.time.Duration>>(Polled.Loading)
    private val load = MutableStateFlow<Polled<LoadAverage>>(Polled.Loading)
    private val memory = MutableStateFlow<Polled<MemoryUsage>>(Polled.Loading)
    private val swap = MutableStateFlow<Polled<SwapUsage>>(Polled.Loading)
    private val containers = MutableStateFlow<Polled<List<ContainerUsage>>>(Polled.Loading)
    private val processes = MutableStateFlow<Polled<List<ProcessUsage>>>(Polled.Loading)
    private val processCount = MutableStateFlow<Polled<ProcessCount>>(Polled.Loading)

    init {
        every { poller.system } returns system
        every { poller.uptime } returns uptime
        every { poller.load } returns load
        every { poller.memory } returns memory
        every { poller.swap } returns swap
        every { poller.containers } returns containers
        every { poller.processes } returns processes
        every { poller.processCount } returns processCount
    }

    /** Drives [primary] through loading, fresh, stale and failed and returns what the use case reported. */
    private suspend fun <P, D> kotlinx.coroutines.test.TestScope.primaryStates(
        useCase: Flow<TelemetryState<D>>,
        primary: MutableStateFlow<Polled<P>>,
        value: P,
    ): List<TelemetryState<D>> {
        var result: List<TelemetryState<D>> = emptyList()
        turbineScope {
            val turbine = useCase.testIn(this)
            runCurrent()
            primary.value = Polled.Value(value, isStale = false)
            runCurrent()
            primary.value = Polled.Value(value, isStale = true)
            runCurrent()
            primary.value = Polled.Failed(offline)
            runCurrent()
            result = turbine.cancelAndConsumeItems()
        }
        return result
    }

    @Test
    fun systemOverviewFollowsThePrimaryPlugin() = runTest {
        val states = primaryStates(ObserveSystemOverviewUseCaseImpl(poller)(), system, systemInfo)
        val data = ObserveSystemOverviewUseCase.Data(systemInfo, null, null)

        assertEquals(
            listOf(
                TelemetryState.Loading,
                TelemetryState.Ready(data, isStale = false),
                TelemetryState.Ready(data, isStale = true),
                TelemetryState.Failed(offline),
            ),
            states,
        )
    }

    @Test
    fun systemOverviewSecondariesOnlyFillTheirOwnFields() = runTest {
        turbineScope {
            system.value = Polled.Value(systemInfo, isStale = false)
            val turbine = ObserveSystemOverviewUseCaseImpl(poller)().testIn(this)
            runCurrent()
            uptime.value = Polled.Value(3.hours, isStale = false)
            runCurrent()
            load.value = Polled.Failed(offline)
            runCurrent()
            uptime.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()
            load.value = Polled.Value(loadAverage, isStale = false)
            runCurrent()

            assertEquals(
                listOf(
                    TelemetryState.Ready(ObserveSystemOverviewUseCase.Data(systemInfo, null, null), false),
                    TelemetryState.Ready(ObserveSystemOverviewUseCase.Data(systemInfo, 3.hours, null), false),
                    TelemetryState.Ready(ObserveSystemOverviewUseCase.Data(systemInfo, 3.hours, null), false),
                    TelemetryState.Ready(ObserveSystemOverviewUseCase.Data(systemInfo, null, null), false),
                    TelemetryState.Ready(ObserveSystemOverviewUseCase.Data(systemInfo, null, loadAverage), false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun memoryFollowsThePrimaryPlugin() = runTest {
        val states = primaryStates(ObserveMemoryUseCaseImpl(poller)(), memory, memoryUsage)
        val data = ObserveMemoryUseCase.Data(memoryUsage, null)

        assertEquals(
            listOf(
                TelemetryState.Loading,
                TelemetryState.Ready(data, isStale = false),
                TelemetryState.Ready(data, isStale = true),
                TelemetryState.Failed(offline),
            ),
            states,
        )
    }

    @Test
    fun memorySwapIsNullWhenMissingFailedOrUnavailable() = runTest {
        turbineScope {
            memory.value = Polled.Value(memoryUsage, isStale = false)
            val turbine = ObserveMemoryUseCaseImpl(poller)().testIn(this)
            runCurrent()
            swap.value = Polled.Value(swapUsage, isStale = false)
            runCurrent()
            swap.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()

            assertEquals(
                listOf(
                    TelemetryState.Ready(ObserveMemoryUseCase.Data(memoryUsage, null), false),
                    TelemetryState.Ready(ObserveMemoryUseCase.Data(memoryUsage, swapUsage), false),
                    TelemetryState.Ready(ObserveMemoryUseCase.Data(memoryUsage, null), false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun containersFollowThePrimaryPluginAndAnEmptyListIsReady() = runTest {
        val states = primaryStates(ObserveContainersUseCaseImpl(poller)(), containers, emptyList())
        val data = ObserveContainersUseCase.Data(emptyList())

        assertEquals(
            listOf(
                TelemetryState.Loading,
                TelemetryState.Ready(data, isStale = false),
                TelemetryState.Ready(data, isStale = true),
                TelemetryState.Failed(offline),
            ),
            states,
        )
    }

    @Test
    fun containersPassTheListThrough() = runTest {
        turbineScope {
            containers.value = Polled.Value(listOf(container), isStale = false)
            val turbine = ObserveContainersUseCaseImpl(poller)().testIn(this)
            runCurrent()

            assertEquals(
                listOf(TelemetryState.Ready(ObserveContainersUseCase.Data(listOf(container)), false)),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun processesFollowThePrimaryPlugin() = runTest {
        val states = primaryStates(ObserveProcessesUseCaseImpl(poller)(), processes, listOf(process))
        val data = ObserveProcessesUseCase.Data(listOf(process), null)

        assertEquals(
            listOf(
                TelemetryState.Loading,
                TelemetryState.Ready(data, isStale = false),
                TelemetryState.Ready(data, isStale = true),
                TelemetryState.Failed(offline),
            ),
            states,
        )
    }

    @Test
    fun processCountIsNullWhenMissingFailedOrUnavailable() = runTest {
        turbineScope {
            processes.value = Polled.Value(listOf(process), isStale = false)
            val turbine = ObserveProcessesUseCaseImpl(poller)().testIn(this)
            runCurrent()
            processCount.value = Polled.Value(count, isStale = false)
            runCurrent()
            processCount.value = Polled.Failed(offline)
            runCurrent()

            assertEquals(
                listOf(
                    TelemetryState.Ready(ObserveProcessesUseCase.Data(listOf(process), null), false),
                    TelemetryState.Ready(ObserveProcessesUseCase.Data(listOf(process), count), false),
                    TelemetryState.Ready(ObserveProcessesUseCase.Data(listOf(process), null), false),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun refreshTelemetryUseCaseReachesThePoller() {
        every { poller.refresh() } just runs

        RefreshTelemetryUseCaseImpl(poller)()

        verifySequence { poller.refresh() }
    }

    companion object {
        private val offline = TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused)
        private val systemInfo = SystemInfo("nas", "Linux", "6.1", "Debian", "x86_64", "NAS")
        private val loadAverage = LoadAverage(0.5f, 0.4f, 0.3f, 4)
        private val memoryUsage = MemoryUsage(100, 40, 60, 55, 10, 5)
        private val swapUsage = SwapUsage(20, 1, 19)
        private val container = ContainerUsage("1", "web", "nginx", "docker", ContainerStatus.Running, 1f, 10, 100, "1 min")
        private val process = ProcessUsage(1, "init", "root", 0.1f, 0.2f, 1024, "S", null)
        private val count = ProcessCount(100, 2, 90, 400)
    }
}
