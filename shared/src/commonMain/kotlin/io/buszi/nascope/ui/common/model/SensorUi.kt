package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.FanSensorData
import io.buszi.nascope.data.telemetry.TemperatureSensorData
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a temperature sensor with its alert thresholds.
 */
@Immutable
data class TemperatureSensorUi(
    val label: String,
    val currentTempC: Float,
    val warningThresholdC: Float,
    val criticalThresholdC: Float,
    val history: List<Float> = emptyList(),
) {
    /** Lowest temperature in [history], or the current one without history. */
    val minHistoryC: Float
        get() = history.minOrNull() ?: currentTempC

    /** Highest temperature in [history], or the current one without history. */
    val maxHistoryC: Float
        get() = history.maxOrNull() ?: currentTempC

    /** Mean temperature of [history], or the current one without history. */
    val averageHistoryC: Float
        get() = if (history.isEmpty()) currentTempC else history.average().toFloat()

    /** Degrees left until [warningThresholdC]; negative once exceeded. */
    val headroomToWarningC: Float
        get() = warningThresholdC - currentTempC

    /** Degrees left until [criticalThresholdC]; negative once exceeded. */
    val headroomToCriticalC: Float
        get() = criticalThresholdC - currentTempC

    /** Status derived from the current temperature and the alert thresholds. */
    val status: StatusLevel
        get() = when {
            currentTempC >= criticalThresholdC -> StatusLevel.Critical
            currentTempC >= warningThresholdC -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

    /** Current temperature normalized to `[0.0, 1.0]` within [rangeC]. */
    fun progressIn(rangeC: ClosedFloatingPointRange<Float>): Float = normalize(currentTempC, rangeC)

    /** Warning threshold normalized to `[0.0, 1.0]` within [rangeC]. */
    fun warningProgressIn(rangeC: ClosedFloatingPointRange<Float>): Float = normalize(warningThresholdC, rangeC)

    /** Critical threshold normalized to `[0.0, 1.0]` within [rangeC]. */
    fun criticalProgressIn(rangeC: ClosedFloatingPointRange<Float>): Float = normalize(criticalThresholdC, rangeC)

    private fun normalize(valueC: Float, rangeC: ClosedFloatingPointRange<Float>): Float =
        ((valueC - rangeC.start) / (rangeC.endInclusive - rangeC.start)).coerceIn(0f, 1f)

    companion object {
        fun from(data: TemperatureSensorData) = TemperatureSensorUi(
            label = data.label,
            currentTempC = data.currentTempC,
            warningThresholdC = data.warningThresholdC,
            criticalThresholdC = data.criticalThresholdC,
            history = data.history,
        )
    }
}

/**
 * UI representation of a fan sensor.
 */
@Immutable
data class FanSensorUi(
    val label: String,
    val rpm: Int,
    val maxRpm: Int = 2200,
    val dutyCyclePercent: Float = if (maxRpm > 0) (rpm.toFloat() / maxRpm).coerceIn(0f, 1f) else 0f,
    val rpmHistory: List<Float> = emptyList(),
) {
    /** A fan that reports no rotation. */
    val isStalled: Boolean
        get() = rpm <= 0

    companion object {
        fun from(data: FanSensorData) = FanSensorUi(
            label = data.label,
            rpm = data.rpm,
            maxRpm = data.maxRpm,
            dutyCyclePercent = data.dutyCyclePercent,
            rpmHistory = data.rpmHistory,
        )
    }
}
