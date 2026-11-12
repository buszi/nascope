package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase.Data
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.toTelemetryState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal class ObserveAlertsUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveAlertsUseCase {

    override fun invoke(): Flow<TelemetryState<Data>> =
        poller.alerts.toTelemetryState(poller.serverTime) { alerts, serverTime ->
            val (active, ended) = alerts.partition { it.isActive }
            Data(
                active = active.sortedWith(compareByDescending<GlancesAlert> { severity(it.state) }
                    .thenByDescending { it.beginEpochSeconds }),
                history = ended.sortedByDescending { it.endEpochSeconds }.take(ObserveAlertsUseCase.HISTORY_LIMIT),
                serverTime = serverTime,
            )
        }

    private fun severity(state: AlertState): Int = when (state) {
        AlertState.Critical -> 3
        AlertState.Warning -> 2
        AlertState.Careful -> 1
        AlertState.Unknown -> 0
    }
}
