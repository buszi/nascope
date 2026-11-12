package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `processlist`. Secondary: `processcount` and, for the logical core count, `cpu`. The full list is returned; limiting is up to the UI. */
fun interface ObserveProcessesUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val processes: List<ProcessUsage>,
        val count: ProcessCount?,
        /** `null` when `cpu` is unavailable: process CPU is then judged as if the host had one core. */
        val logicalCores: Int? = null,
    )
}
