package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.math.abs
import kotlin.math.round

/** UI representation of the CPU shown by the CPU brick and its details screen. */
@Immutable
data class CpuUi(
    val modelName: String = "",
    val usagePercent: Float = 0f,
    /** Signed change against the recent average (`+2.4%`); `null` hides the trend badge. */
    val trend: String? = null,
    /** `null` when the host does not report the clock: the clock is not shown. */
    val frequencyGhz: Float? = null,
    val maxFrequencyGhz: Float? = null,
    /** CPU package temperature in Celsius; `null` when no CPU sensor is available: the tag is not shown. */
    val temperatureC: Float? = null,
    val cores: List<CpuCoreUi> = emptyList(),
    /** Total usage in percent, oldest first. */
    val usageHistory: List<Float> = emptyList(),
) {
    val busiestCore: CpuCoreUi?
        get() = cores.maxByOrNull { it.usagePercent }

    val usageHistoryAverage: Float
        get() = if (usageHistory.isEmpty()) 0f else usageHistory.average().toFloat()

    val usageHistoryPeak: Float
        get() = usageHistory.maxOrNull() ?: 0f

    val temperatureStatus: StatusLevel
        get() = temperatureC?.let(CpuCoreUi::temperatureStatus) ?: StatusLevel.Operational

    val overallStatus: StatusLevel
        get() = CpuCoreUi.overallStatus(usagePercent, temperatureC ?: 0f)

    companion object {
        private const val HzPerGhz = 1_000_000_000.0

        fun from(data: ObserveCpuUseCase.Data): CpuUi = CpuUi(
            modelName = data.info?.name.orEmpty(),
            usagePercent = data.usage.totalPercent,
            trend = data.usageDelta?.let(::formatSignedPercent),
            frequencyGhz = data.info?.currentFrequencyHz?.let { (it / HzPerGhz).toFloat() },
            maxFrequencyGhz = data.info?.maxFrequencyHz?.let { (it / HzPerGhz).toFloat() },
            temperatureC = data.temperature,
            cores = data.cores.map(CpuCoreUi::from),
            usageHistory = data.usageHistory,
        )

        /** `+2.4%` / `-1.0%`, one decimal. */
        fun formatSignedPercent(delta: Float): String {
            val tenths = round(delta * 10).toInt()
            val sign = if (tenths < 0) "-" else "+"
            return "$sign${abs(tenths) / 10}.${abs(tenths) % 10}%"
        }
    }
}
