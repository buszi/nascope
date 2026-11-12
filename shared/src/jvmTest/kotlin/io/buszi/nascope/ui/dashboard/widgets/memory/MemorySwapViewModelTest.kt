package io.buszi.nascope.ui.dashboard.widgets.memory

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.MemorySwapUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MemorySwapViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeMemory = mockk<ObserveMemoryUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeMemory() } returns states
        MemorySwapViewModel(
            observeMemory = observeMemory,
            refreshTelemetry = refreshTelemetry,
        )
    }

    private suspend fun kotlinx.coroutines.test.TestScope.viewStates(
        vararg steps: TelemetryState<Data>,
    ): List<ViewState> {
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
    fun memorySwapViewModelStartsLoadingThenShowsTheMappedData() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState),
            viewStates(TelemetryState.Ready(data, isStale = false)),
        )
    }

    @Test
    fun memorySwapViewModelMarksStaleDataAndKeepsTheContent() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState, readyViewState.copy(widgetState = WidgetState.Stale)),
            viewStates(TelemetryState.Ready(data, false), TelemetryState.Ready(data, true)),
        )
    }

    @Test
    fun memorySwapViewModelShowsUnavailableWhenThePluginIsMissing() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(widgetState = WidgetState.Unavailable)),
            viewStates(TelemetryState.Failed(TelemetryFailure.PluginUnavailable)),
        )
    }

    @Test
    fun memorySwapViewModelTreatsAMissingSwapAsNoSwapAndKeepsMissingCacheFigures() = runTest {
        val noSwap = Data(memory.copy(cachedBytes = null, buffersBytes = null), swap = null)

        val last = viewStates(TelemetryState.Ready(noSwap, false)).last()

        assertEquals(
            MemorySwapUi(totalBytes = 100, usedBytes = 40, cachedBytes = null, buffersBytes = null, freeBytes = 60),
            last.memory,
        )
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun memorySwapViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeMemory()
            refreshTelemetry()
        }
    }

    companion object {
        private val memory = MemoryUsage(100, 40, 60, 55, 10, 5)
        private val data = Data(memory, SwapUsage(20, 2, 18))

        private val readyViewState = ViewState(
            memory = MemorySwapUi(
                totalBytes = 100,
                usedBytes = 40,
                cachedBytes = 10,
                buffersBytes = 5,
                freeBytes = 60,
                totalSwapBytes = 20,
                usedSwapBytes = 2,
                freeSwapBytes = 18,
            ),
            widgetState = WidgetState.Ready,
        )
    }
}
