package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/** Primary: `system`. Secondary: `uptime`, `load`. */
fun interface ObserveSystemOverviewUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val system: SystemInfo,
        val uptime: Duration?,
        val load: LoadAverage?,
    )
}
