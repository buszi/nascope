package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.SensorsUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SensorsThermalsViewModel(
    private val observeSensors: ObserveSensorsUseCase,
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
        observeSensors().onEach { state ->
            mutableViewState.update {
                it.copy(
                    sensors = when (state) {
                        is TelemetryState.Ready -> SensorsUi.from(state.data)
                        TelemetryState.Loading -> SensorsUi()
                        is TelemetryState.Failed -> it.sensors
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val sensors: SensorsUi = SensorsUi(),
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
