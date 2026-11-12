package io.buszi.nascope.domain.model.telemetry

import io.buszi.nascope.domain.model.connection.ConnectionFailure

sealed interface TelemetryFailure {
    /** Transport, TLS, 401 or not a Glances API. */
    data class Connection(val failure: ConnectionFailure) : TelemetryFailure

    /** The plugin is missing or disabled on this server. */
    data object PluginUnavailable : TelemetryFailure

    /** The server answered 200, but the body cannot be read at all. */
    data class InvalidResponse(val detail: String) : TelemetryFailure
}
