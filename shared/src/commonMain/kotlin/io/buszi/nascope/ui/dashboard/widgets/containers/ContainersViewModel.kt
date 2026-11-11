package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.ContainersData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.ToggleExpand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ContainersViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> retry()
            ToggleExpand -> toggleExpand()
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.containers
            .onEach { data ->
                mutableViewState.update { current ->
                    deriveViewState(data, current.isExpanded)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deriveViewState(data: ContainersData, isExpanded: Boolean): ViewState {
        val containers = data.containers.map(ContainerUi::from)
        val overallStatus = when {
            containers.any { it.state == ContainerStateUi.Restarting } -> StatusLevel.Critical
            containers.any { it.state == ContainerStateUi.Stopped || it.state == ContainerStateUi.Paused } -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        return ViewState(
            engine = data.engine,
            containers = containers,
            runningCount = data.runningCount,
            pausedCount = data.pausedCount,
            stoppedCount = data.stoppedCount,
            totalCount = data.totalCount,
            isExpanded = isExpanded,
            overallStatus = overallStatus,
            widgetState = WidgetState.Ready,
        )
    }

    private fun toggleExpand() {
        mutableViewState.update { current ->
            current.copy(isExpanded = !current.isExpanded)
        }
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data object ToggleExpand : ViewAction
    }

    @Stable
    data class ViewState(
        val engine: String = "Docker",
        val containers: List<ContainerUi> = emptyList(),
        val runningCount: Int = 0,
        val pausedCount: Int = 0,
        val stoppedCount: Int = 0,
        val totalCount: Int = 0,
        val isExpanded: Boolean = false,
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val widgetState: WidgetState = WidgetState.Loading,
    )
}
