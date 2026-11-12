package io.buszi.nascope.ui.dashboard.widgets.containers

import io.buszi.nascope.domain.model.connection.ConnectionFailure
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ContainersViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeContainers = mockk<ObserveContainersUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeContainers() } returns states
        ContainersViewModel(observeContainers = observeContainers, refreshTelemetry = refreshTelemetry)
    }

    private fun TestScope.emit(vararg containers: ContainerUsage, stale: Boolean = false) {
        states.value = TelemetryState.Ready(Data(containers.toList()), stale)
        runCurrent()
    }

    @Test
    fun containersViewModelStartsLoadingThenShowsContainersWithCountsAndEngine() = runTest {
        viewModel
        assertEquals(ViewState(), viewModel.viewState.value)

        emit(usage("a", ContainerStatus.Running), usage("b", ContainerStatus.Paused), usage("c", ContainerStatus.Exited))

        val state = viewModel.viewState.value
        assertEquals("Docker", state.engine)
        assertEquals(3, state.totalCount)
        assertEquals(1, state.runningCount)
        assertEquals(1, state.pausedCount)
        assertEquals(1, state.stoppedCount)
        assertEquals(StatusLevel.Degraded, state.overallStatus)
        assertEquals(WidgetState.Ready, state.widgetState)
    }

    @Test
    fun containersViewModelMapsEveryContainerStatusToAUiState() = runTest {
        viewModel
        emit(*ContainerStatus.entries.map { usage(it.name, it) }.toTypedArray())

        assertEquals(
            listOf(
                ContainerStateUi.Running, ContainerStateUi.Paused, ContainerStateUi.Restarting, ContainerStateUi.Exited,
                ContainerStateUi.Created, ContainerStateUi.Dead, ContainerStateUi.Unknown,
            ),
            viewModel.viewState.value.containers.map { it.state },
        )
        assertEquals(StatusLevel.Critical, viewModel.viewState.value.overallStatus)
    }

    @Test
    fun containersViewModelKeepsMissingMetricsNullAndLeavesOutTheEngineTag() = runTest {
        viewModel
        emit(usage("a", ContainerStatus.Exited, engine = null, cpu = null, memory = null, uptime = null, image = null))

        val container = viewModel.viewState.value.containers.single()
        assertNull(container.cpuPercent)
        assertNull(container.memoryUsageBytes)
        assertNull(container.uptime)
        assertEquals("", container.image)
        assertEquals("", viewModel.viewState.value.engine)
    }

    @Test
    fun containersViewModelShowsNoContainersAsReady() = runTest {
        viewModel
        emit()

        assertEquals(ViewState(widgetState = WidgetState.Ready), viewModel.viewState.value)
    }

    @Test
    fun containersViewModelToggleExpandSwitchesTheExpansion() = runTest {
        viewModel
        emit(usage("a", ContainerStatus.Running))

        viewModel.onViewAction(ToggleExpand)
        assertEquals(true, viewModel.viewState.value.isExpanded)
        viewModel.onViewAction(ToggleExpand)
        assertEquals(false, viewModel.viewState.value.isExpanded)
    }

    @Test
    fun containersViewModelMarksStaleKeepsContainersAndMapsFailures() = runTest {
        viewModel
        emit(usage("a", ContainerStatus.Running), stale = true)
        assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)
        assertEquals(1, viewModel.viewState.value.containers.size)

        states.value = TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout))
        runCurrent()
        assertIs<WidgetState.Offline>(viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Loading
        runCurrent()
        assertEquals(ViewState(), viewModel.viewState.value)
    }

    @Test
    fun containersViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeContainers()
            refreshTelemetry()
        }
    }

    private companion object {
        fun usage(
            id: String,
            status: ContainerStatus,
            engine: String? = "docker",
            cpu: Float? = 1f,
            memory: Long? = 100L,
            uptime: String? = "Up",
            image: String? = "img",
        ) = ContainerUsage(id, id, image, engine, status, cpu, memory, 200L, uptime)
    }
}
