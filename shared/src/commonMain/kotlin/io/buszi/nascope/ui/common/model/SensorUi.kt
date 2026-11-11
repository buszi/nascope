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
) {
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
) {
    companion object {
        fun from(data: FanSensorData) = FanSensorUi(
            label = data.label,
            rpm = data.rpm,
            maxRpm = data.maxRpm,
            dutyCyclePercent = data.dutyCyclePercent,
        )
    }
}
