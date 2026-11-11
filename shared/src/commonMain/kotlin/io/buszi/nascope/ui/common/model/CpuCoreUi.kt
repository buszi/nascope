package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.CpuCoreData
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a single CPU core.
 *
 * @param index Zero-based logical core index.
 * @param usagePercent Utilization in `[0, 100]`.
 */
@Immutable
data class CpuCoreUi(
    val index: Int,
    val usagePercent: Float,
    val frequencyGhz: Float,
    val temperatureC: Float,
) {
    /** Utilization normalized to `[0.0, 1.0]`. */
    val usageRatio: Float
        get() = (usagePercent / 100f).coerceIn(0f, 1f)

    /** Status derived from [usagePercent]. */
    val usageStatus: StatusLevel
        get() = usageStatus(usagePercent)

    /** Status derived from [temperatureC]. */
    val temperatureStatus: StatusLevel
        get() = temperatureStatus(temperatureC)

    companion object {
        const val UsageWarningPercent: Float = 75f
        const val UsageCriticalPercent: Float = 90f
        const val TemperatureWarningC: Float = 75f
        const val TemperatureCriticalC: Float = 85f

        fun from(index: Int, data: CpuCoreData) = CpuCoreUi(
            index = index,
            usagePercent = data.usagePercent,
            frequencyGhz = data.frequencyGhz,
            temperatureC = data.temperatureC,
        )

        fun usageStatus(usagePercent: Float): StatusLevel = when {
            usagePercent >= UsageCriticalPercent -> StatusLevel.Critical
            usagePercent >= UsageWarningPercent -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        fun temperatureStatus(temperatureC: Float): StatusLevel = when {
            temperatureC >= TemperatureCriticalC -> StatusLevel.Critical
            temperatureC >= TemperatureWarningC -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        /** Worst of the [usageStatus] and the [temperatureStatus]. */
        fun overallStatus(usagePercent: Float, temperatureC: Float): StatusLevel =
            maxOf(usageStatus(usagePercent), temperatureStatus(temperatureC))
    }
}
