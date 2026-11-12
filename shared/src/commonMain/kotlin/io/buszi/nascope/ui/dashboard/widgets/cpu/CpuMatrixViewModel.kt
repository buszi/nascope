package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.CpuUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CpuMatrixViewModel(
    private val observeCpu: ObserveCpuUseCase,
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
        observeCpu().onEach { state ->
            mutableViewState.update {
                it.copy(
                    cpu = when (state) {
                        is TelemetryState.Ready -> CpuUi.from(state.data)
                        TelemetryState.Loading -> CpuUi()
                        is TelemetryState.Failed -> it.cpu
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val cpu: CpuUi = CpuUi(),
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
