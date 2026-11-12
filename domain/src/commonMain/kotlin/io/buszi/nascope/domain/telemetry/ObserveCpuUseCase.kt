package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `cpu`. Secondary: `percpu`, `quicklook`, `sensors`. History: total usage %. */
fun interface ObserveCpuUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val usage: CpuUsage,
        /** Empty when `percpu` is not available. */
        val cores: List<CpuCoreUsage>,
        val info: CpuInfo?,
        /** Package temperature, else the hottest core, in the unit Glances reports. */
        val temperature: Float?,
        /** Total usage in percent, oldest first, at most 60 samples. */
        val usageHistory: List<Float>,
        /** Current usage minus the mean of [usageHistory]; `null` with fewer than 10 samples. */
        val usageDelta: Float?,
    )
}
