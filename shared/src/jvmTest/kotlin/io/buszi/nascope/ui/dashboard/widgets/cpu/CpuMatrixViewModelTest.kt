package io.buszi.nascope.ui.dashboard.widgets.cpu

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.CpuCoreData
import io.buszi.nascope.data.telemetry.CpuMatrixData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewState
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
class CpuMatrixViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        CpuMatrixViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun cpuMatrixViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            val cpuMatrixFlow = MutableStateFlow(cpuMatrix)
            every { telemetryStateHolder.cpuMatrix } returns cpuMatrixFlow

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
            }
        }
    }

    @Test
    fun cpuMatrixViewModelHandlesRetryAction() = runTest {
        turbineScope {
            val cpuMatrixFlow = MutableStateFlow(cpuMatrix)
            every { telemetryStateHolder.cpuMatrix } returns cpuMatrixFlow
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
                telemetryStateHolder.cpuMatrix
                telemetryStateHolder.refresh()
            }
        }
    }

    companion object {
        private val cpuMatrix = CpuMatrixData(
            modelName = "AMD Ryzen 9 5950X (16-Core)",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.80f,
            temperatureC = 58.0f,
            cores = listOf(
                CpuCoreData(34.2f),
                CpuCoreData(48.1f),
                CpuCoreData(12.0f),
                CpuCoreData(88.5f),
            ),
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            modelName = "AMD Ryzen 9 5950X (16-Core)",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.80f,
            temperatureC = 58.0f,
            coreUsages = listOf(34.2f, 48.1f, 12.0f, 88.5f),
            widgetState = WidgetState.Ready,
        )
    }
}
