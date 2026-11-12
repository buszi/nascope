package io.buszi.nascope.domain.telemetry.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.foreground.AppForegroundState
import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.domain.telemetry.DemoTelemetrySource
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.TelemetryPollerImpl
import io.buszi.nascope.domain.telemetry.TelemetrySource
import io.buszi.nascope.domain.telemetry.TelemetrySourceProvider
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryUseCasesTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.dispatcher)
    private val poller = mockk<TelemetryPoller>()

    private val cpu = MutableStateFlow<Polled<CpuUsage>>(Polled.Loading)
    private val cpuCores = MutableStateFlow<Polled<List<CpuCoreUsage>>>(Polled.Loading)
    private val sensors = MutableStateFlow<Polled<List<SensorReading>>>(Polled.Loading)
    private val fileSystems = MutableStateFlow<Polled<List<FileSystemUsage>>>(Polled.Loading)
    private val diskIo = MutableStateFlow<Polled<List<DiskIoRate>>>(Polled.Loading)
    private val network = MutableStateFlow<Polled<List<NetworkInterfaceUsage>>>(Polled.Loading)
    private val gpus = MutableStateFlow<Polled<List<GpuUsage>>>(Polled.Loading)

    init {
        every { poller.cpu } returns cpu
        every { poller.cpuCores } returns cpuCores
        every { poller.cpuInfo } returns MutableStateFlow(Polled.Loading)
        every { poller.sensors } returns sensors
        every { poller.fileSystems } returns fileSystems
        every { poller.diskIo } returns diskIo
        every { poller.networkInterfaces } returns network
        every { poller.gpus } returns gpus
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    private val cpuUseCase by lazy { ObserveCpuUseCaseImpl(poller, applicationScope, coroutineRule.dispatcher) }
    private val storageUseCase by lazy { ObserveStorageUseCaseImpl(poller, applicationScope, coroutineRule.dispatcher) }
    private val networkUseCase by lazy { ObserveNetworkUseCaseImpl(poller, applicationScope, coroutineRule.dispatcher) }
    private val gpuUseCase by lazy { ObserveGpuUseCaseImpl(poller, applicationScope, coroutineRule.dispatcher) }
    private val sensorsUseCase by lazy { ObserveSensorsUseCaseImpl(poller, applicationScope, coroutineRule.dispatcher) }

    private fun TestScope.fresh(percent: Float) {
        cpu.value = Polled.Value(usage(percent), isStale = false)
        runCurrent()
    }

    private fun List<TelemetryState<ObserveCpuUseCase.Data>>.cpuData() =
        filterIsInstance<TelemetryState.Ready<ObserveCpuUseCase.Data>>().map { it.data }

    // CPU

    @Test
    fun cpuHistoryGrowsPerFreshValueAndIsNotAppendedWhenStale() = runTest {
        turbineScope {
            val turbine = cpuUseCase().testIn(this)
            runCurrent()
            fresh(10f)
            fresh(20f)
            cpu.value = Polled.Value(usage(20f), isStale = true)
            runCurrent()
            fresh(30f)

            assertEquals(
                listOf(listOf(10f), listOf(10f, 20f), listOf(10f, 20f), listOf(10f, 20f, 30f)),
                turbine.cancelAndConsumeItems().cpuData().map { it.usageHistory },
            )
        }
    }

    @Test
    fun cpuHistoryClearsOnLoading() = runTest {
        turbineScope {
            val turbine = cpuUseCase().testIn(this)
            runCurrent()
            fresh(10f)
            fresh(20f)
            cpu.value = Polled.Loading
            runCurrent()
            fresh(30f)

            val states = turbine.cancelAndConsumeItems()
            assertEquals(TelemetryState.Loading, states[3])
            assertEquals(listOf(30f), states.cpuData().last().usageHistory)
        }
    }

    @Test
    fun cpuHistoryIsSharedByTwoCollectors() = runTest {
        turbineScope {
            val first = cpuUseCase().testIn(this)
            val second = cpuUseCase().testIn(this)
            runCurrent()
            fresh(10f)
            fresh(20f)

            val firstStates = first.cancelAndConsumeItems()
            assertEquals(firstStates, second.cancelAndConsumeItems())
            assertEquals(listOf(10f, 20f), firstStates.cpuData().last().usageHistory)
        }
    }

    @Test
    fun cpuUsageDeltaIsNullBelowTenSamplesAndTheDifferenceToTheMeanFromTen() = runTest {
        turbineScope {
            val turbine = cpuUseCase().testIn(this)
            runCurrent()
            (1..9).forEach { fresh(it.toFloat()) }
            fresh(20f)

            val deltas = turbine.cancelAndConsumeItems().cpuData().map { it.usageDelta }
            assertEquals(List(9) { null }, deltas.take(9))
            assertEquals(13.5f, deltas.last()) // 20 - mean(1..9, 20) = 20 - 6.5
        }
    }

    @Test
    fun cpuSecondaryDataFillsItsFieldsAndFailuresOnlyEmptyThem() = runTest {
        turbineScope {
            val turbine = cpuUseCase().testIn(this)
            runCurrent()
            fresh(10f)
            cpuCores.value = Polled.Value(listOf(CpuCoreUsage(0, 5f)), isStale = false)
            runCurrent()
            cpuCores.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()

            val data = turbine.cancelAndConsumeItems().cpuData()
            assertEquals(listOf(emptyList(), listOf(CpuCoreUsage(0, 5f)), emptyList()), data.map { it.cores })
            assertEquals(listOf(null, null, null), data.map { it.info })
        }
    }

    @Test
    fun cpuTemperatureIsThePackageThenTheHottestCoreThenNull() = runTest {
        turbineScope {
            val turbine = cpuUseCase().testIn(this)
            runCurrent()
            fresh(10f)
            sensors.value = Polled.Value(listOf(temp("Core 0", 80f), temp("Package id 0", 55f)), isStale = false)
            runCurrent()
            sensors.value = Polled.Value(listOf(temp("Core 0", 50f), temp("Core 1", 62f)), isStale = false)
            runCurrent()
            sensors.value = Polled.Value(listOf(temp("acpitz 0", 30f)), isStale = false)
            runCurrent()
            sensors.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()

            assertEquals(
                listOf(null, 55f, 62f, null, null),
                turbine.cancelAndConsumeItems().cpuData().map { it.temperature },
            )
        }
    }

    // Storage

    @Test
    fun storageSumsDiskRatesAndKeepsHistoryPerFreshDiskIoPoll() = runTest {
        turbineScope {
            val turbine = storageUseCase().testIn(this)
            runCurrent()
            fileSystems.value = Polled.Value(listOf(mount), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 100, 50), DiskIoRate("b", 1, 2)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 10, 5)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 10, 5)), isStale = true)
            runCurrent()

            val data = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveStorageUseCase.Data>>().map { it.data }
            assertEquals(listOf(null, 101L, 10L, 10L), data.map { it.readBytesPerSec })
            assertEquals(listOf(null, 52L, 5L, 5L), data.map { it.writeBytesPerSec })
            assertEquals(listOf(101f, 10f), data.last().readHistory.takeLast(2))
            assertEquals(2, data.last().readHistory.size)
            assertEquals(2, data.last().writeHistory.size)
            assertEquals(listOf(mount), data.last().fileSystems)
        }
    }

    @Test
    fun storageKeepsAHistoryPerDiskDropsVanishedDisksAndDoesNotAppendStalePolls() = runTest {
        turbineScope {
            val turbine = storageUseCase().testIn(this)
            runCurrent()
            fileSystems.value = Polled.Value(listOf(mount), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 100, 50), DiskIoRate("b", 1, 2)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 10, 5), DiskIoRate("b", 3, 4)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 10, 5), DiskIoRate("b", 3, 4)), isStale = true)
            runCurrent()
            val grown = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveStorageUseCase.Data>>().map { it.data }.last()
            assertEquals(listOf("a", "b"), grown.disks.map { it.io.diskName })
            assertEquals(listOf(100f, 10f), grown.disks[0].readHistory)
            assertEquals(listOf(2f, 4f), grown.disks[1].writeHistory)
        }
        // A disk that vanishes loses its buffer; when it returns it starts again.
        turbineScope {
            val turbine = storageUseCase().testIn(this)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 1, 1), DiskIoRate("b", 7, 7)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 2, 2)), isStale = false)
            runCurrent()
            diskIo.value = Polled.Value(listOf(DiskIoRate("a", 3, 3), DiskIoRate("b", 9, 9)), isStale = false)
            runCurrent()
            val last = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveStorageUseCase.Data>>().map { it.data }.last()
            assertEquals(listOf(1f, 2f, 3f), last.disks[0].readHistory)
            assertEquals(listOf(9f), last.disks[1].readHistory)
        }
    }

    @Test
    fun storageWithoutDiskIoStaysReadyWithEmptyDiskFields() = runTest {
        turbineScope {
            fileSystems.value = Polled.Value(listOf(mount), isStale = false)
            diskIo.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            val turbine = storageUseCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(TelemetryState.Ready(ObserveStorageUseCase.Data(listOf(mount), emptyList(), null, null, emptyList(), emptyList()), false)),
                turbine.cancelAndConsumeItems(),
            )
        }
    }

    // Network

    @Test
    fun networkKeepsHistoryPerInterfaceAndDropsTheBufferOfAVanishedInterface() = runTest {
        turbineScope {
            val turbine = networkUseCase().testIn(this)
            runCurrent()
            network.value = Polled.Value(listOf(nic("eth0", rx = 1), nic("eth1", rx = 100)), isStale = false)
            runCurrent()
            network.value = Polled.Value(listOf(nic("eth0", rx = 2)), isStale = false)
            runCurrent()
            network.value = Polled.Value(listOf(nic("eth0", rx = 3), nic("eth1", rx = 101)), isStale = false)
            runCurrent()

            val last = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveNetworkUseCase.Data>>().last().data
            assertEquals(listOf(1f, 2f, 3f), last.interfaces.first { it.usage.name == "eth0" }.rxHistory)
            assertEquals(listOf(101f), last.interfaces.first { it.usage.name == "eth1" }.rxHistory)
        }
    }

    // GPU

    @Test
    fun gpuKeepsUtilizationHistoryPerGpuAndNoneWhenTheDriverReportsNoUtilization() = runTest {
        turbineScope {
            val turbine = gpuUseCase().testIn(this)
            runCurrent()
            fun gpu(id: String, load: Float?) = GpuUsage(id, id, load, null, null, null)
            gpus.value = Polled.Value(listOf(gpu("a", 10f), gpu("b", null)), isStale = false)
            runCurrent()
            gpus.value = Polled.Value(listOf(gpu("a", 20f), gpu("b", null)), isStale = false)
            runCurrent()
            gpus.value = Polled.Value(listOf(gpu("a", 20f)), isStale = true)
            runCurrent()
            gpus.value = Polled.Value(emptyList(), isStale = false)
            runCurrent()
            gpus.value = Polled.Value(listOf(gpu("a", 5f)), isStale = false)
            runCurrent()

            val data = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveGpuUseCase.Data>>().map { it.data }
            assertEquals(listOf(10f, 20f), data[1].gpus.first { it.usage.id == "a" }.utilizationHistory)
            assertEquals(emptyList(), data[1].gpus.first { it.usage.id == "b" }.utilizationHistory)
            assertEquals(listOf(10f, 20f), data[2].gpus.single().utilizationHistory) // stale: not appended
            assertEquals(emptyList(), data[3].gpus) // no GPU: ready and empty
            assertEquals(listOf(5f), data[4].gpus.single().utilizationHistory) // vanished: starts over
        }
    }

    // Sensors

    @Test
    fun sensorsAreGroupedByLabelAndKeepHistoryPerLabel() = runTest {
        turbineScope {
            val turbine = sensorsUseCase().testIn(this)
            runCurrent()
            val readings = listOf(
                temp("Package id 0", 50f),
                temp("Composite", 40f),
                temp("acpitz 0", 30f),
                SensorReading("fan1", SensorKind.FanSpeed, 1200f, "R", null, null),
            )
            sensors.value = Polled.Value(readings, isStale = false)
            runCurrent()
            sensors.value = Polled.Value(readings.map { it.copy(value = it.value + 1) }, isStale = false)
            runCurrent()
            sensors.value = Polled.Value(readings.take(1), isStale = false)
            runCurrent()
            sensors.value = Polled.Value(readings, isStale = false)
            runCurrent()

            val data = turbine.cancelAndConsumeItems()
                .filterIsInstance<TelemetryState.Ready<ObserveSensorsUseCase.Data>>().map { it.data }
            assertEquals(listOf("Package id 0"), data[0].cpu.map { it.reading.label })
            assertEquals(listOf("Composite"), data[0].drives.map { it.reading.label })
            assertEquals(listOf("fan1"), data[0].fans.map { it.reading.label })
            assertEquals(listOf("acpitz 0"), data[0].other.map { it.reading.label })
            assertEquals(listOf(50f, 51f), data[1].cpu.single().history)
            assertEquals(listOf(50f, 51f, 50f), data[2].cpu.single().history)
            assertEquals(listOf(40f), data.last().drives.single().history) // dropped while missing, starts over
        }
    }

    // Demo source: the 61st poll drops the oldest

    @Test
    fun withTheDemoSourceSixtyPollsGiveSixtyPointsAndTheSixtyFirstDropsTheOldest() = runTest {
        turbineScope {
            val source: TelemetrySource = DemoTelemetrySource(Random(7))
            val provider = mockk<TelemetrySourceProvider> { every { this@mockk.source } returns MutableStateFlow(source) }
            val foreground = mockk<AppForegroundState> { every { isForeground } returns MutableStateFlow(true) }
            val interval = mockk<ObserveRefreshIntervalUseCase> {
                every { this@mockk() } returns MutableStateFlow(RefreshInterval.S1)
            }
            val realPoller = TelemetryPollerImpl(provider, foreground, interval, applicationScope, coroutineRule.dispatcher)
            val useCase = ObserveCpuUseCaseImpl(realPoller, applicationScope, coroutineRule.dispatcher)

            val turbine = useCase().testIn(this)
            runCurrent()
            advanceTimeBy(60_000.milliseconds) // polls at 0 s ... 60 s: 61 polls
            runCurrent()

            val histories = turbine.cancelAndConsumeItems().cpuData().map { it.usageHistory }
            assertEquals(61, histories.size)
            assertEquals(List(60) { it + 1 }, histories.map { it.size }.take(60))
            assertEquals(60, histories[59].size)
            assertEquals(60, histories[60].size)
            assertEquals(histories[59].drop(1), histories[60].dropLast(1))
        }
    }

    companion object {
        private fun usage(percent: Float) = CpuUsage(percent, null, null, null, null, null)
        private fun temp(label: String, value: Float) =
            SensorReading(label, SensorKind.CpuTemperature, value, "C", null, null)

        private fun nic(name: String, rx: Long) = NetworkInterfaceUsage(name, null, true, null, rx, 0, 0, 0)
        private val mount = FileSystemUsage("/", "sda1", "ext4", 100, 40, 60, null)
    }
}
