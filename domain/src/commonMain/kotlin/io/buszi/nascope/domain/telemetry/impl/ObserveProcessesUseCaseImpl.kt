package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveProcessesUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveProcessesUseCase {
    override fun invoke(): Flow<TelemetryState<Data>> = poller.processes.toTelemetryState(poller.processCount, poller.cpu) { processes, count, cpu ->
        Data(processes, count, cpu?.logicalCores)
    }
}
