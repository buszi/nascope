package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnSelectFilter
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction.OnToggleContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the containers details screen: every container (no top-N cut), filterable by state,
 * with per-container resource gauges and a recent log preview on expansion.
 */
@KoinViewModel
class ContainersDetailsViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnSelectFilter -> mutableViewState.update { it.copy(stateFilter = action.state) }
            is OnToggleContainer -> toggleContainer(action.containerId)
        }
    }

    private fun toggleContainer(containerId: String) {
        mutableViewState.update { state ->
            state.copy(
                expandedContainerId = if (state.expandedContainerId == containerId) null else containerId,
            )
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.containers.onEach { data ->
            mutableViewState.update { current ->
                val containers = data.containers.map(ContainerUi::from)
                current.copy(
                    engine = data.engine,
                    containers = containers,
                    expandedContainerId = current.expandedContainerId
                        ?.takeIf { expanded -> containers.any { it.id == expanded } },
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val engine: String = "",
        val containers: List<ContainerUi> = emptyList(),
        /** Only containers in this state are listed; `null` lists all. */
        val stateFilter: ContainerStateUi? = null,
        val expandedContainerId: String? = null,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val filteredContainers: List<ContainerUi>
            get() = stateFilter?.let { filter -> containers.filter { it.state == filter } } ?: containers

        fun countOf(state: ContainerStateUi): Int = containers.count { it.state == state }

        /** Combined CPU usage of the running containers. */
        val totalCpuPercent: Float
            get() = containers.filter { it.state == ContainerStateUi.Running }.sumOf { it.cpuPercent.toDouble() }.toFloat()

        /** Combined memory usage of all containers. */
        val totalMemoryBytes: Long
            get() = containers.sumOf { it.memoryUsageBytes }

        val overallStatus: StatusLevel
            get() = containers.maxOfOrNull { it.state.status } ?: StatusLevel.Operational
    }

    sealed interface ViewAction {
        data class OnSelectFilter(val state: ContainerStateUi?) : ViewAction
        data class OnToggleContainer(val containerId: String) : ViewAction
    }
}
