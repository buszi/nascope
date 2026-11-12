package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `containers`. */
fun interface ObserveContainersUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val containers: List<ContainerUsage>,
    )
}
