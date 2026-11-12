package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** One entry of the Glances alert log as the alerts brick and its details show it. */
@Immutable
data class AlertUi(
    val id: String,
    /** Readable metric label, e.g. `MEMORY`; a type this app does not know is shown as Glances names it. */
    val typeLabel: String,
    val state: AlertState,
    val isActive: Boolean,
    /** Glances' description of the alert, e.g. "High swap (paging) usage". */
    val message: String?,
    /**
     * How long an active alert has been running, measured on the server's clock; `null` when the server's time is
     * not known (the device clock is never used). For an ended alert: how long it lasted.
     */
    val duration: String?,
    val min: String?,
    val avg: String?,
    val max: String?,
    val topProcesses: List<String>,
) {
    /** Glances has three severities and the app three levels: careful and warning are both degraded. */
    val status: StatusLevel
        get() = when (state) {
            AlertState.Critical -> StatusLevel.Critical
            AlertState.Careful, AlertState.Warning, AlertState.Unknown -> StatusLevel.Degraded
        }

    companion object {
        fun from(alert: GlancesAlert, serverTimeEpochSeconds: Long?): AlertUi {
            val endOrNow = alert.endEpochSeconds ?: serverTimeEpochSeconds
            return AlertUi(
                id = "${alert.type}-${alert.beginEpochSeconds}",
                typeLabel = typeLabel(alert.type),
                state = alert.state,
                isActive = alert.isActive,
                message = alert.message,
                duration = endOrNow?.let { formatDuration((it - alert.beginEpochSeconds).coerceAtLeast(0)) },
                min = alert.min?.let(::formatFigure),
                avg = alert.avg?.let(::formatFigure),
                max = alert.max?.let(::formatFigure),
                topProcesses = alert.topProcesses,
            )
        }

        private val TypeLabels = mapOf(
            "CPU" to "CPU",
            "CPU_USER" to "CPU USER",
            "CPU_SYSTEM" to "CPU SYSTEM",
            "CPU_IOWAIT" to "CPU I/O WAIT",
            "CPU_STEAL" to "CPU STEAL",
            "LOAD" to "LOAD",
            "MEM" to "MEMORY",
            "MEMSWAP" to "SWAP",
        )

        fun typeLabel(type: String): String = TypeLabels[type] ?: type

        /** `14d 06h 32m`, `3h 05m`, `12m`, and `<1m` for less than a minute. */
        fun formatDuration(seconds: Long): String =
            if (seconds < 60) "<1m" else SystemOverviewUi.formatUptime(seconds.seconds)

        private fun formatFigure(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()
    }
}

/** The dashboard header badge: how many alerts are active and the worst of them. */
@Immutable
data class AlertBadgeUi(val count: Int, val status: StatusLevel) {
    companion object {
        /** `null` when nothing is active: no badge. */
        fun from(alerts: AlertsUi): AlertBadgeUi? =
            alerts.worstStatus?.let { AlertBadgeUi(alerts.activeCount, it) }
    }
}

/** The alert log: active alerts (worst first) and the recent history (newest first). */
@Immutable
data class AlertsUi(
    val active: List<AlertUi> = emptyList(),
    val history: List<AlertUi> = emptyList(),
) {
    val activeCount: Int get() = active.size

    /** Worst state among the active alerts; `null` when there are none. */
    val worstStatus: StatusLevel? get() = active.maxOfOrNull { it.status }

    companion object {
        fun from(data: ObserveAlertsUseCase.Data): AlertsUi {
            val now = data.serverTime?.epochSeconds
            return AlertsUi(
                active = data.active.map { AlertUi.from(it, now) },
                history = data.history.map { AlertUi.from(it, now) },
            )
        }
    }
}
