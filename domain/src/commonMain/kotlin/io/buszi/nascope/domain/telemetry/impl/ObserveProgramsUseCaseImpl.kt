package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProgramsUseCase
import io.buszi.nascope.domain.telemetry.ObserveProgramsUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveProgramsUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveProgramsUseCase {
    override fun invoke(): Flow<TelemetryState<Data>> =
        poller.programs.toTelemetryState(poller.cpu) { programs, cpu -> Data(programs, cpu?.logicalCores) }
}
