package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/**
 * Primary: `programlist` (Glances 4 only; `Failed(PluginUnavailable)` elsewhere). Secondary: `cpu`, for the logical
 * core count. A loop exists only while this flow is collected, so collect it only while programs are shown.
 */
fun interface ObserveProgramsUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val programs: List<ProgramUsage>,
        /** `null` when `cpu` is unavailable: program CPU is then judged as if the host had one core. */
        val logicalCores: Int? = null,
    )
}
