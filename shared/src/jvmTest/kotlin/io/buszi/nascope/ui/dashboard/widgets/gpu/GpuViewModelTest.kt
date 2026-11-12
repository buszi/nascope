package io.buszi.nascope.ui.dashboard.widgets.gpu

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.GpuUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewState
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

@OptIn(ExperimentalCoroutinesApi::class)
class GpuViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeGpu = mockk<ObserveGpuUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeGpu() } returns states
        GpuViewModel(observeGpu = observeGpu, refreshTelemetry = refreshTelemetry)
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
    fun gpuViewModelStartsLoadingThenShowsTheMappedGpus() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState),
            viewStates(TelemetryState.Ready(data, isStale = false)),
        )
    }

    @Test
    fun gpuViewModelShowsAHostWithoutGpuAsReadyAndEmpty() = runTest {
        val last = viewStates(TelemetryState.Ready(Data(emptyList()), isStale = false)).last()

        assertEquals(emptyList(), last.gpus)
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun gpuViewModelMarksStaleDataAndKeepsTheContent() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState, readyViewState.copy(widgetState = WidgetState.Stale)),
            viewStates(TelemetryState.Ready(data, false), TelemetryState.Ready(data, true)),
        )
    }

    @Test
    fun gpuViewModelShowsUnavailableWhenThePluginIsMissing() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(widgetState = WidgetState.Unavailable)),
            viewStates(TelemetryState.Failed(TelemetryFailure.PluginUnavailable)),
        )
    }

    @Test
    fun gpuViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeGpu()
            refreshTelemetry()
        }
    }

    companion object {
        private val data = Data(
            listOf(ObserveGpuUseCase.Gpu(GpuUsage("intel0", "Intel GPU", 12f, null, null, null), listOf(5f, 12f))),
        )

        private val readyViewState = ViewState(
            gpus = listOf(GpuUi("intel0", "Intel GPU", 12f, null, null, null, listOf(5f, 12f))),
            widgetState = WidgetState.Ready,
        )
    }
}
