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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the sensors details screen: a read-only thermal and cooling breakdown with history,
 * min / average / max and threshold headroom for every sensor.
 */
@KoinViewModel
class SensorsDetailsViewModel(
    private val observeSensors: ObserveSensorsUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
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
}
