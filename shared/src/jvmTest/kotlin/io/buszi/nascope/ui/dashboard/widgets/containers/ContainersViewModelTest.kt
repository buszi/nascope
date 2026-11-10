package io.buszi.nascope.ui.dashboard.widgets.containers

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState
import io.buszi.nascope.data.telemetry.ContainersData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.ToggleExpand
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewState
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContainersViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        ContainersViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun containersViewModelEmitsReadyStateWithTelemetry() = runTest {
        turbineScope {
            val containersFlow = MutableStateFlow(testContainersData)
            every { telemetryStateHolder.containers } returns containersFlow

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
                telemetryStateHolder.containers
            }
        }
    }

    @Test
    fun toggleExpandSwitchesExpansionState() = runTest {
        turbineScope {
            val containersFlow = MutableStateFlow(testContainersData)
            every { telemetryStateHolder.containers } returns containersFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertFalse(viewModel.viewState.value.isExpanded)

            viewModel.onViewAction(ToggleExpand)
            runCurrent()

            assertTrue(viewModel.viewState.value.isExpanded)

            viewModel.onViewAction(ToggleExpand)
            runCurrent()

            assertFalse(viewModel.viewState.value.isExpanded)
            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restartingContainerTriggersCriticalOverallStatus() = runTest {
        turbineScope {
            val criticalData = testContainersData.copy(
                containers = listOf(
                    testRunningContainer,
                    testRunningContainer.copy(id = "restarting-1", state = ContainerState.Restarting),
                ),
            )
            val containersFlow = MutableStateFlow(criticalData)
            every { telemetryStateHolder.containers } returns containersFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(StatusLevel.Critical, states.last().overallStatus)
        }
    }

    @Test
    fun retryInvokesStateHolderRefresh() = runTest {
        turbineScope {
            val containersFlow = MutableStateFlow(testContainersData)
            every { telemetryStateHolder.containers } returns containersFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnRetry)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.containers
                telemetryStateHolder.refresh()
            }
        }
    }

    private companion object {
        val testRunningContainer = ContainerInfo(
            id = "7f3b8a1c",
            name = "traefik",
            image = "traefik:v3.1",
            state = ContainerState.Running,
            cpuPercent = 1.4f,
            memoryUsageBytes = 48_500_000L,
            memoryLimitBytes = 256_000_000L,
            uptime = "Up 14 days",
        )

        val testPausedContainer = ContainerInfo(
            id = "d58e310a",
            name = "qbittorrent",
            image = "linuxserver/qbittorrent:latest",
            state = ContainerState.Paused,
            cpuPercent = 0.0f,
            memoryUsageBytes = 112_000_000L,
            memoryLimitBytes = 1_024_000_000L,
            uptime = "Paused",
        )

        val testStoppedContainer = ContainerInfo(
            id = "1a84f67c",
            name = "borgmatic-backup",
            image = "b3vis/borgmatic:latest",
            state = ContainerState.Stopped,
            cpuPercent = 0.0f,
            memoryUsageBytes = 0L,
            memoryLimitBytes = 512_000_000L,
            uptime = "Exited (0) 6h ago",
        )

        val testContainersData = ContainersData(
            engine = "Docker 27.0.3",
            containers = listOf(
                testRunningContainer,
                testPausedContainer,
                testStoppedContainer,
            ),
        )

        val initialViewState = ViewState()

        val readyViewState = ViewState(
            engine = "Docker 27.0.3",
            containers = listOf(
                testRunningContainer,
                testPausedContainer,
                testStoppedContainer,
            ),
            runningCount = 1,
            pausedCount = 1,
            stoppedCount = 1,
            totalCount = 3,
            isExpanded = false,
            overallStatus = StatusLevel.Degraded,
            widgetState = WidgetState.Ready,
        )
    }
}
