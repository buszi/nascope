package io.buszi.nascope.ui.dashboard.widgets.cpu

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.CpuCoreData
import io.buszi.nascope.data.telemetry.CpuMatrixData
import io.buszi.nascope.data.telemetry.HostOverviewData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CpuDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        CpuDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun cpuDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.cpuMatrix } returns MutableStateFlow(cpuMatrix)
            every { telemetryStateHolder.hostOverview } returns MutableStateFlow(hostOverview)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.cpuMatrix
                telemetryStateHolder.hostOverview
            }
        }
    }

    @Test
    fun cpuDetailsViewModelSortsCores() = runTest {
        turbineScope {
            every { telemetryStateHolder.cpuMatrix } returns MutableStateFlow(cpuMatrix)
            every { telemetryStateHolder.hostOverview } returns MutableStateFlow(hostOverview)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(listOf(0, 1, 2), viewModel.viewState.value.sortedCores.map { it.index })

            viewModel.onViewAction(OnSelectSort(SortOrder.Usage))
            assertEquals(listOf(2, 0, 1), viewModel.viewState.value.sortedCores.map { it.index })

            viewModel.onViewAction(OnSelectSort(SortOrder.Frequency))
            assertEquals(listOf(2, 1, 0), viewModel.viewState.value.sortedCores.map { it.index })

            viewModel.onViewAction(OnSelectSort(SortOrder.Temperature))
            assertEquals(listOf(2, 0, 1), viewModel.viewState.value.sortedCores.map { it.index })

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cpuDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(10f / 3f, state.averageFrequencyGhz, absoluteTolerance = 0.001f)
        assertEquals(4.5f, state.peakFrequencyGhz)
        assertEquals(90f, state.hottestCoreTemperatureC)
        assertEquals(2, state.busiestCore?.index)
        assertEquals(40f, state.usageHistoryAverage)
        assertEquals(60f, state.usageHistoryPeak)
        assertEquals(StatusLevel.Operational, state.temperatureStatus)
        assertEquals(StatusLevel.Operational, state.overallStatus)
        assertEquals(0.25f, state.loadPerCore(0.75f))
    }

    companion object {
        private val cpuMatrix = CpuMatrixData(
            modelName = "Test CPU",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.0f,
            temperatureC = 58.0f,
            cores = listOf(
                CpuCoreData(usagePercent = 50f, frequencyGhz = 2.0f, temperatureC = 60f),
                CpuCoreData(usagePercent = 10f, frequencyGhz = 3.5f, temperatureC = 50f),
                CpuCoreData(usagePercent = 95f, frequencyGhz = 4.5f, temperatureC = 90f),
            ),
            usageHistory = listOf(20f, 40f, 60f),
        )

        private val hostOverview = HostOverviewData(
            hostname = "host",
            osName = "os",
            kernelRelease = "kernel",
            uptime = "1d",
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            cpuCores = 3,
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            modelName = "Test CPU",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.0f,
            temperatureC = 58.0f,
            cores = cpuMatrix.cores.mapIndexed(CpuCoreUi::from),
            usageHistory = listOf(20f, 40f, 60f),
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            widgetState = WidgetState.Ready,
        )
    }
}
