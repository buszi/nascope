package io.buszi.nascope.ui.dashboard.widgets.containers

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnSelectFilter
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnToggleContainer
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
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

    private val observeContainers = mockk<ObserveContainersUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeContainers() } returns states
        ContainersDetailsViewModel(observeContainers = observeContainers, refreshTelemetry = refreshTelemetry)
    }

    private fun TestScope.emit(containers: List<ContainerUsage>, stale: Boolean = false) {
        states.value = TelemetryState.Ready(Data(containers), stale)
        runCurrent()
    }

    @Test
    fun containersDetailsViewModelStartsLoadingThenShowsAllContainers() = runTest {
        viewModel
        assertEquals(ViewState(), viewModel.viewState.value)

        emit(containers)

        assertEquals(
            ViewState(engine = "Docker", containers = containers.map(ContainerUi::from), widgetState = WidgetState.Ready),
            viewModel.viewState.value,
        )
    }

    @Test
    fun containersDetailsViewModelFiltersByState() = runTest {
        viewModel
        emit(containers)

        viewModel.onViewAction(OnSelectFilter(ContainerStateUi.Running))
        assertEquals(listOf("a1", "b2"), viewModel.viewState.value.filteredContainers.map { it.id })
        viewModel.onViewAction(OnSelectFilter(ContainerStateUi.Restarting))
        assertEquals(emptyList(), viewModel.viewState.value.filteredContainers)
        viewModel.onViewAction(OnSelectFilter(null))
        assertEquals(3, viewModel.viewState.value.filteredContainers.size)
    }

    @Test
    fun containersDetailsViewModelTogglesTheExpandedContainerAndCollapsesOneThatDisappears() = runTest {
        viewModel
        emit(containers)

        viewModel.onViewAction(OnToggleContainer("a1"))
        assertEquals("a1", viewModel.viewState.value.expandedContainerId)
        viewModel.onViewAction(OnToggleContainer("b2"))
        assertEquals("b2", viewModel.viewState.value.expandedContainerId)
        viewModel.onViewAction(OnToggleContainer("b2"))
        assertNull(viewModel.viewState.value.expandedContainerId)

        viewModel.onViewAction(OnToggleContainer("c3"))
        emit(containers.filter { it.id != "c3" })
        assertNull(viewModel.viewState.value.expandedContainerId)
    }

    @Test
    fun containersDetailsViewModelMarksStaleAndShowsUnavailable() = runTest {
        viewModel
        emit(containers, stale = true)
        assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)
        assertEquals(3, viewModel.viewState.value.containers.size)
    }

    @Test
    fun containersDetailsViewStateComputesAggregatesIgnoringMissingMetrics() {
        val state = ViewState(
            containers = containers.map(ContainerUi::from) + ContainerUi("d4", "x", "", ContainerStateUi.Running, null, null),
        )

        assertEquals(3, state.countOf(ContainerStateUi.Running))
        assertEquals(1, state.countOf(ContainerStateUi.Exited))
        assertEquals(0, state.countOf(ContainerStateUi.Paused))
        assertEquals(5.0f, state.totalCpuPercent, absoluteTolerance = 0.001f)
        assertEquals(300L, state.totalMemoryBytes)
        assertEquals(StatusLevel.Degraded, state.overallStatus)
    }

    private companion object {
        val containers = listOf(
            ContainerUsage("a1", "web", "nginx", "docker", ContainerStatus.Running, 3.0f, 100L, 200L, "Up 1 day"),
            ContainerUsage("b2", "db", "postgres", "docker", ContainerStatus.Running, 2.0f, 200L, null, "Up 2 days"),
            ContainerUsage("c3", "job", "borg", "docker", ContainerStatus.Exited, null, null, 50L, null),
        )
    }
}
