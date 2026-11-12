package io.buszi.nascope.domain.telemetry

import kotlinx.coroutines.flow.Flow

internal interface TelemetrySourceProvider {
    /** Follows the session: Connected -> live source for that profile, Demo -> demo source, NotConfigured -> null. */
    val source: Flow<TelemetrySource?>
}
