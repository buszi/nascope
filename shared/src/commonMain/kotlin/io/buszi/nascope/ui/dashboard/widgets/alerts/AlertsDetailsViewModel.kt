package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.AlertsUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/** Drives the alerts details screen: the active alerts and the full recent history with min / avg / max. */
@KoinViewModel
class AlertsDetailsViewModel(
    private val observeAlerts: ObserveAlertsUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeAlerts().onEach { state ->
            mutableViewState.update {
                it.copy(
                    alerts = when (state) {
                        is TelemetryState.Ready -> AlertsUi.from(state.data)
                        TelemetryState.Loading -> AlertsUi()
                        is TelemetryState.Failed -> it.alerts
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val alerts: AlertsUi = AlertsUi(),
        val widgetState: WidgetState = WidgetState.Loading,
    )
}
