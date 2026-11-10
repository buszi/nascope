package io.buszi.nascope.ui.dashboard.widgets.system

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.HostOverviewData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewState
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
class SystemOverviewViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        SystemOverviewViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun systemOverviewViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            val hostOverviewFlow = MutableStateFlow(hostOverview)
            every { telemetryStateHolder.hostOverview } returns hostOverviewFlow

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
                telemetryStateHolder.hostOverview
            }
        }
    }

    @Test
    fun systemOverviewViewModelHandlesRetryAction() = runTest {
        turbineScope {
            val hostOverviewFlow = MutableStateFlow(hostOverview)
            every { telemetryStateHolder.hostOverview } returns hostOverviewFlow
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
                telemetryStateHolder.hostOverview
                telemetryStateHolder.refresh()
            }
        }
    }

    companion object {
        private val hostOverview = HostOverviewData(
            hostname = "alpha-node-01.local",
            osName = "Ubuntu 24.04.1 LTS",
            kernelRelease = "Linux 6.8.0-45-generic",
            uptime = "14d 06h 32m",
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            cpuCores = 8,
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            hostname = "alpha-node-01.local",
            osName = "Ubuntu 24.04.1 LTS",
            kernelRelease = "Linux 6.8.0-45-generic",
            uptime = "14d 06h 32m",
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            cpuCores = 8,
            platform = HostPlatform.Ubuntu,
            widgetState = WidgetState.Ready,
        )
    }
}
