package io.buszi.nascope.ui.dashboard.widgets.gpu

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.GpuUi
import io.buszi.nascope.ui.common.model.overallStatus
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class GpuViewModel(
    private val observeGpu: ObserveGpuUseCase,
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
        }
    }

    private fun observeTelemetry() {
        observeGpu().onEach { state ->
            mutableViewState.update {
                it.copy(
                    gpus = when (state) {
                        is TelemetryState.Ready -> GpuUi.from(state.data)
                        TelemetryState.Loading -> emptyList()
                        is TelemetryState.Failed -> it.gpus
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        /** Empty on a host without a GPU Glances can read. */
        val gpus: List<GpuUi> = emptyList(),
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val overallStatus: StatusLevel
            get() = gpus.overallStatus
    }

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
