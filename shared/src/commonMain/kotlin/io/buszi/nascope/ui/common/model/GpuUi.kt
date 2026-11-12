package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of one GPU shown by the GPU brick.
 *
 * Every figure but the name is `null` when the driver does not report it (integrated GPUs rarely report memory,
 * temperature or a fan): the brick leaves such figures out.
 */
@Immutable
data class GpuUi(
    val id: String,
    val name: String,
    val utilizationPercent: Float?,
    val memoryPercent: Float?,
    val temperatureC: Float?,
    val fanSpeedPercent: Float?,
    /** Utilization in percent, oldest first. */
    val utilizationHistory: List<Float> = emptyList(),
) {
    /** Worst of the utilization, memory and temperature statuses; nominal when nothing is reported. */
    val status: StatusLevel
        get() = maxOf(
            utilizationPercent?.let(CpuCoreUi::usageStatus) ?: StatusLevel.Operational,
            memoryPercent?.let(CpuCoreUi::usageStatus) ?: StatusLevel.Operational,
            temperatureC?.let(CpuCoreUi::temperatureStatus) ?: StatusLevel.Operational,
        )

    companion object {
        fun from(data: ObserveGpuUseCase.Gpu) = GpuUi(
            id = data.usage.id,
            name = data.usage.name,
            utilizationPercent = data.usage.utilizationPercent,
            memoryPercent = data.usage.memoryPercent,
            temperatureC = data.usage.temperatureC,
            fanSpeedPercent = data.usage.fanSpeedPercent,
            utilizationHistory = data.utilizationHistory,
        )

        fun from(data: ObserveGpuUseCase.Data): List<GpuUi> = data.gpus.map(::from)
    }
}

/** Worst [GpuUi.status] across the GPUs, [StatusLevel.Operational] when there are none. */
val List<GpuUi>.overallStatus: StatusLevel
    get() = maxOfOrNull { it.status } ?: StatusLevel.Operational
