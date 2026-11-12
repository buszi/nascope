package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import org.koin.core.annotation.Factory

@Factory
internal class RefreshTelemetryUseCaseImpl(
    private val poller: TelemetryPoller,
) : RefreshTelemetryUseCase {
    override fun invoke() = poller.refresh()
}
