package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.runtime.Immutable
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * Encapsulates UNIX system load averages over 1-minute, 5-minute, and 15-minute intervals.
 *
 * @param oneMinute Average system load over the preceding 1 minute.
 * @param fiveMinute Average system load over the preceding 5 minutes.
 * @param fifteenMinute Average system load over the preceding 15 minutes.
 * @param cpuCores Number of logical CPU cores on the host, used for load threshold normalization.
 */
@Immutable
data class LoadAverages(
    val oneMinute: Float,
    val fiveMinute: Float,
    val fifteenMinute: Float,
    val cpuCores: Int = 1,
) {
    /**
     * Normalized load ratio for the 1-minute average relative to available CPU cores.
     * Values <= 1.0 indicate healthy capacity, while > 1.0 indicates queued processes.
     */
    val oneMinuteRatio: Float
        get() = if (cpuCores > 0) oneMinute / cpuCores else oneMinute

    /**
     * Normalized load ratio for the 5-minute average relative to available CPU cores.
     */
    val fiveMinuteRatio: Float
        get() = if (cpuCores > 0) fiveMinute / cpuCores else fiveMinute

    /**
     * Normalized load ratio for the 15-minute average relative to available CPU cores.
     */
    val fifteenMinuteRatio: Float
        get() = if (cpuCores > 0) fifteenMinute / cpuCores else fifteenMinute

    /**
     * Evaluates the telemetry [StatusLevel] for a given load reading against core capacity.
     */
    fun statusFor(load: Float): StatusLevel {
        val ratio = if (cpuCores > 0) load / cpuCores else load
        return when {
            ratio >= 1.0f -> StatusLevel.Critical
            ratio >= 0.75f -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }
    }

    /**
     * Aggregate health status of the load averages based on the short-term 1-minute reading.
     */
    val status: StatusLevel
        get() = statusFor(oneMinute)
}
