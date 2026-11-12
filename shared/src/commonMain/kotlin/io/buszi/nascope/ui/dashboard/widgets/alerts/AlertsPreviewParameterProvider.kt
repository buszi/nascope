package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.ui.common.model.AlertUi
import io.buszi.nascope.ui.common.model.AlertsUi
import io.buszi.nascope.ui.common.util.Message
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsViewModel.ViewState

/**
 * Preview parameter provider for the alerts brick: all clear, one and two warnings, a critical one among others,
 * many alerts (overflow), and the loading, stale, unavailable, error and offline lifecycle states.
 */
class AlertsPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        AllClear,
        OneWarning,
        TwoWarnings,
        CriticalAmongOthers,
        ManyAlerts,
        LoadingState,
        StaleState,
        UnavailableState,
        ErrorState,
        OfflineState,
    )

    companion object {
        fun alert(
            type: String,
            label: String,
            state: AlertState,
            message: String? = null,
            duration: String? = "25m",
            active: Boolean = true,
        ) = AlertUi(
            id = "$type-1", typeLabel = label, state = state, isActive = active, message = message, duration = duration,
            min = "71.2", avg = "73.5", max = "76.4", topProcesses = emptyList(),
        )

        private val swap = alert("MEMSWAP", "SWAP", AlertState.Warning, "High swap (paging) usage")
        private val mem = alert("MEM", "MEMORY", AlertState.Warning, "High swap (paging) usage", "1h 12m")
        private val cpu = alert("CPU_USER", "CPU USER", AlertState.Critical, "High CPU user usage", "4m")
        private val load = alert("LOAD", "LOAD", AlertState.Careful, "Load average above 1 per core", "2h 03m")

        val AllClear = ViewState(widgetState = WidgetState.Ready)
        val OneWarning = ViewState(alerts = AlertsUi(active = listOf(swap)), widgetState = WidgetState.Ready)
        val TwoWarnings = ViewState(alerts = AlertsUi(active = listOf(swap, mem)), widgetState = WidgetState.Ready)
        val CriticalAmongOthers = ViewState(alerts = AlertsUi(active = listOf(cpu, swap, load)), widgetState = WidgetState.Ready)
        val ManyAlerts = ViewState(
            alerts = AlertsUi(active = listOf(cpu, swap, mem, load, alert("X", "X", AlertState.Unknown))),
            widgetState = WidgetState.Ready,
        )
        val LoadingState = ViewState(widgetState = WidgetState.Loading)
        val StaleState = TwoWarnings.copy(widgetState = WidgetState.Stale)
        val UnavailableState = ViewState(widgetState = WidgetState.Unavailable)
        val ErrorState = ViewState(
            widgetState = WidgetState.Error(
                message = Message.plain("Failed to fetch alerts from Glances daemon"),
                onRetry = {},
            ),
        )
        val OfflineState = ViewState(
            widgetState = WidgetState.Offline(reason = Message.plain("Alerts offline"), onReconnect = {}),
        )
    }
}
