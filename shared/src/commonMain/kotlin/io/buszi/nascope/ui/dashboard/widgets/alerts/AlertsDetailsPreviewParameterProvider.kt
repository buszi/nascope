package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.ui.common.model.AlertsUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsDetailsViewModel.ViewState
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsPreviewParameterProvider.Companion.alert

/** Preview parameters of the alerts details: active and ended alerts, only history, all clear, loading. */
class AlertsDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        ViewState(
            alerts = AlertsUi(
                active = listOf(
                    alert("CPU_USER", "CPU USER", AlertState.Critical, "High CPU user usage", "4m")
                        .copy(topProcesses = listOf("ffmpeg", "python3")),
                    alert("MEMSWAP", "SWAP", AlertState.Warning, "High swap (paging) usage", "1h 12m"),
                ),
                history = listOf(
                    alert("LOAD", "LOAD", AlertState.Careful, null, "10m", active = false),
                    alert("MEM", "MEMORY", AlertState.Warning, "High memory usage", "2h 03m", active = false),
                ),
            ),
            widgetState = WidgetState.Ready,
        ),
        ViewState(
            alerts = AlertsUi(history = listOf(alert("LOAD", "LOAD", AlertState.Careful, null, "10m", active = false))),
            widgetState = WidgetState.Ready,
        ),
        ViewState(widgetState = WidgetState.Ready),
        ViewState(widgetState = WidgetState.Loading),
    )
}
