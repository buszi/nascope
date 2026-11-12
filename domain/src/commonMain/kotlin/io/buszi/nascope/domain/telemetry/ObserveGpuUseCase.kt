package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/**
 * Primary: `gpu`. History: utilization in percent per GPU id.
 *
 * A host without a GPU Glances can read is `Ready` with an empty list; a host without the plugin is `Failed` with
 * `PluginUnavailable`.
 */
fun interface ObserveGpuUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val gpus: List<Gpu>,
    )

    data class Gpu(
        val usage: GpuUsage,
        /** Utilization in percent, oldest first, at most 60 samples; empty while the driver reports none. */
        val utilizationHistory: List<Float>,
    )
}
