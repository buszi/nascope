package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `mem`. Secondary: `memswap`. */
fun interface ObserveMemoryUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val memory: MemoryUsage,
        val swap: SwapUsage?,
    )
}
