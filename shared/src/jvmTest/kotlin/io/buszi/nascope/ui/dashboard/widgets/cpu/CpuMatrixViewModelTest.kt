package io.buszi.nascope.ui.dashboard.widgets.cpu

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.model.CpuUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewAction.OnRetry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CpuMatrixViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeCpu = mockk<ObserveCpuUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeCpu() } returns states
        CpuMatrixViewModel(
            observeCpu = observeCpu,
            refreshTelemetry = refreshTelemetry,
        )
    }

    private suspend fun TestScope.viewStates(vararg steps: TelemetryState<Data>): List<ViewState> {
        var result: List<ViewState> = emptyList()
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            steps.forEach {
                states.value = it
                runCurrent()
            }
            result = turbine.cancelAndConsumeItems()
        }
        return result
    }

    @Test
    fun cpuMatrixViewModelStartsLoadingThenShowsTheMappedCpu() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(cpu = fullCpu, widgetState = WidgetState.Ready)),
            viewStates(TelemetryState.Ready(data, isStale = false)),
        )
    }

    @Test
    fun cpuMatrixViewModelHidesMissingSecondaryDataAndTheTrendBadge() = runTest {
        val bare = Data(usage, emptyList(), null, null, listOf(10f), null)

        val last = viewStates(TelemetryState.Ready(bare, isStale = false)).last()

        assertNull(last.cpu.trend)
        assertNull(last.cpu.frequencyGhz)
        assertNull(last.cpu.temperatureC)
        assertEquals(emptyList(), last.cpu.cores)
        assertEquals(listOf(10f), last.cpu.usageHistory)
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun cpuMatrixViewModelMarksStaleDataAndKeepsTheContent() = runTest {
        val states = viewStates(TelemetryState.Ready(data, false), TelemetryState.Ready(data, true))

        assertEquals(WidgetState.Stale, states.last().widgetState)
        assertEquals(fullCpu, states.last().cpu)
    }

    @Test
    fun cpuMatrixViewModelMapsFailureStates() = runTest {
        val states = viewStates(
            TelemetryState.Failed(TelemetryFailure.PluginUnavailable),
            TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
            TelemetryState.Failed(TelemetryFailure.InvalidResponse("bad")),
        )

        assertEquals(WidgetState.Unavailable, states[1].widgetState)
        assertIs<WidgetState.Offline>(states[2].widgetState)
        assertIs<WidgetState.Error>(states[3].widgetState)
    }

    @Test
    fun cpuMatrixViewModelDropsTheOldContentWhenLoadingAgain() = runTest {
        val states = viewStates(TelemetryState.Ready(data, false), TelemetryState.Loading)

        assertEquals(ViewState(), states.last())
    }

    @Test
    fun cpuMatrixViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeCpu()
            refreshTelemetry()
        }
    }

    companion object {
        private val usage = CpuUsage(42.8f, null, null, null, null, 3)

        private val data = Data(
            usage = usage,
            cores = listOf(CpuCoreUsage(0, 50f), CpuCoreUsage(1, 10f), CpuCoreUsage(2, 95f)),
            info = CpuInfo("Ryzen 9", 3_800_000_000, 4_900_000_000),
            temperature = 58f,
            usageHistory = listOf(40f, 45f),
            usageDelta = 2.44f,
        )

        private val fullCpu = CpuUi(
            modelName = "Ryzen 9",
            usagePercent = 42.8f,
            trend = "+2.4%",
            frequencyGhz = 3.8f,
            maxFrequencyGhz = 4.9f,
            temperatureC = 58f,
            cores = listOf(CpuCoreUi(0, 50f), CpuCoreUi(1, 10f), CpuCoreUi(2, 95f)),
            usageHistory = listOf(40f, 45f),
        )
    }
}
