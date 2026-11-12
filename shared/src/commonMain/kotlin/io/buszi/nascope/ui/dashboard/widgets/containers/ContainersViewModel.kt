package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.common.model.ContainersUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.ToggleExpand
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ContainersViewModel(
    private val observeContainers: ObserveContainersUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> refreshTelemetry()
            ToggleExpand -> mutableViewState.update { it.copy(isExpanded = !it.isExpanded) }
        }
    }

    private fun observeTelemetry() {
        observeContainers()
            .onEach { state ->
                mutableViewState.update { current ->
                    val containers = when (state) {
                        is TelemetryState.Ready -> ContainersUi.from(state.data)
                        TelemetryState.Loading -> ContainersUi()
                        is TelemetryState.Failed -> ContainersUi(current.engine, current.containers)
                    }
                    current.copy(
                        engine = containers.engine,
                        containers = containers.containers,
                        widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data object ToggleExpand : ViewAction
    }

    @Stable
    data class ViewState(
        val engine: String = "",
        val containers: List<ContainerUi> = emptyList(),
        val isExpanded: Boolean = false,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val runningCount: Int
            get() = containers.count { it.state == ContainerStateUi.Running }

        val pausedCount: Int
            get() = containers.count { it.state == ContainerStateUi.Paused }

        /** Everything that is neither running nor paused: exited, created, dead, restarting and unknown. */
        val stoppedCount: Int
            get() = totalCount - runningCount - pausedCount

        val totalCount: Int
            get() = containers.size

        val overallStatus: StatusLevel
            get() = containers.maxOfOrNull { it.state.status } ?: StatusLevel.Operational
    }
}
