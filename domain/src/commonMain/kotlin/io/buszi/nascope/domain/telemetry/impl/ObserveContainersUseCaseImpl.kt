package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveContainersUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveContainersUseCase {
    override fun invoke(): Flow<TelemetryState<Data>> = poller.containers.toTelemetryState(::Data)
}
