package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveMemoryUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveMemoryUseCase {
    override fun invoke(): Flow<TelemetryState<Data>> = poller.memory.toTelemetryState(poller.swap, ::Data)
}
