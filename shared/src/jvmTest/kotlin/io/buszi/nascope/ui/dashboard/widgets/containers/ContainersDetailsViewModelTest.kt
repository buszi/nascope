package io.buszi.nascope.ui.dashboard.widgets.containers

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState
import io.buszi.nascope.data.telemetry.ContainersData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnSelectFilter
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnToggleContainer
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewState
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ContainersDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        ContainersDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun containersDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.containers } returns MutableStateFlow(containersData)

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
    fun containersDetailsViewModelFiltersByState() = runTest {
        turbineScope {
            every { telemetryStateHolder.containers } returns MutableStateFlow(containersData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(3, viewModel.viewState.value.filteredContainers.size)

            viewModel.onViewAction(OnSelectFilter(ContainerStateUi.Running))
            assertEquals(listOf("web", "db"), viewModel.viewState.value.filteredContainers.map { it.name })

            viewModel.onViewAction(OnSelectFilter(ContainerStateUi.Restarting))
            assertEquals(emptyList(), viewModel.viewState.value.filteredContainers)

            viewModel.onViewAction(OnSelectFilter(null))
            assertEquals(3, viewModel.viewState.value.filteredContainers.size)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun containersDetailsViewModelTogglesExpandedContainer() = runTest {
        turbineScope {
            every { telemetryStateHolder.containers } returns MutableStateFlow(containersData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnToggleContainer("a1"))
            assertEquals("a1", viewModel.viewState.value.expandedContainerId)

            viewModel.onViewAction(OnToggleContainer("b2"))
            assertEquals("b2", viewModel.viewState.value.expandedContainerId)

            viewModel.onViewAction(OnToggleContainer("b2"))
            assertNull(viewModel.viewState.value.expandedContainerId)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun containersDetailsViewModelCollapsesContainerThatDisappears() = runTest {
        turbineScope {
            val containersFlow = MutableStateFlow(containersData)
            every { telemetryStateHolder.containers } returns containersFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnToggleContainer("c3"))

            containersFlow.value = containersData.copy(containers = containersData.containers.filter { it.id != "c3" })
            runCurrent()

            assertNull(viewModel.viewState.value.expandedContainerId)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun containersDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(2, state.countOf(ContainerStateUi.Running))
        assertEquals(1, state.countOf(ContainerStateUi.Stopped))
        assertEquals(0, state.countOf(ContainerStateUi.Paused))
        assertEquals(5.0f, state.totalCpuPercent, absoluteTolerance = 0.001f)
        assertEquals(300L, state.totalMemoryBytes)
        assertEquals(StatusLevel.Degraded, state.overallStatus)
    }

    companion object {
        private val containersData = ContainersData(
            engine = "Docker 27.0.3",
            containers = listOf(
                ContainerInfo("a1", "web", "nginx", ContainerState.Running, 3.0f, 100L, 200L, "Up 1 day", listOf("log a")),
                ContainerInfo("b2", "db", "postgres", ContainerState.Running, 2.0f, 200L, null, "Up 2 days"),
                ContainerInfo("c3", "job", "borg", ContainerState.Stopped, 0.0f, 0L, 50L, "Exited (0)"),
            ),
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            engine = "Docker 27.0.3",
            containers = containersData.containers.map(ContainerUi::from),
            widgetState = WidgetState.Ready,
        )
    }
}
