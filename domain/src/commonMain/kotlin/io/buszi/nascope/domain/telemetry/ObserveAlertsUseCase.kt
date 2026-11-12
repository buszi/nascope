package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Primary: `alert`. Secondary: `now`.
 *
 * Alert times come from the server's clock, so the server's own time is delivered alongside, taken from the `now`
 * plugin; the device clock is never used. [Data.serverTime] is `null` while `now` is loading or failed, and then no
 * duration can be shown.
 */
fun interface ObserveAlertsUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        /** Alerts still active, the worst state first and, within a state, the most recent first. */
        val active: List<GlancesAlert>,
        /** Ended alerts, the most recently ended first, at most [HISTORY_LIMIT]. */
        val history: List<GlancesAlert>,
        val serverTime: Instant?,
    )

    companion object {
        const val HISTORY_LIMIT = 50
    }
}
