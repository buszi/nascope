package io.buszi.nascope.ui.dashboard.widgets.memory

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.MemorySwapData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
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

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        MemorySwapViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun memorySwapViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            val memorySwapFlow = MutableStateFlow(memorySwap)
            every { telemetryStateHolder.memorySwap } returns memorySwapFlow

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
                telemetryStateHolder.memorySwap
            }
        }
    }

    @Test
    fun memorySwapViewModelHandlesRetryAction() = runTest {
        turbineScope {
            val memorySwapFlow = MutableStateFlow(memorySwap)
            every { telemetryStateHolder.memorySwap } returns memorySwapFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnRetry)

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.memorySwap
                telemetryStateHolder.refresh()
            }
        }
    }

    companion object {
        private val memorySwap = MemorySwapData(
            totalMemBytes = 34_359_738_368L,
            usedMemBytes = 13_743_895_347L,
            cachedMemBytes = 9_126_805_504L,
            buffersMemBytes = 1_288_490_188L,
            freeMemBytes = 10_200_547_329L,
            totalSwapBytes = 17_179_869_184L,
            usedSwapBytes = 2_576_980_377L,
            freeSwapBytes = 14_602_888_807L,
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            totalMemBytes = 34_359_738_368L,
            usedMemBytes = 13_743_895_347L,
            cachedMemBytes = 9_126_805_504L,
            buffersMemBytes = 1_288_490_188L,
            freeMemBytes = 10_200_547_329L,
            totalSwapBytes = 17_179_869_184L,
            usedSwapBytes = 2_576_980_377L,
            freeSwapBytes = 14_602_888_807L,
            widgetState = WidgetState.Ready,
        )
    }
}
