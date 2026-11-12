package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveSystemOverviewUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveSystemOverviewUseCase {
    override fun invoke(): Flow<TelemetryState<Data>> =
        poller.system.toTelemetryState(poller.uptime, poller.load, ::Data)
}
