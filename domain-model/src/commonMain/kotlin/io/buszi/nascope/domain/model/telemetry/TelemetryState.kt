package io.buszi.nascope.domain.model.telemetry

/** What an observing telemetry use case reports. */
sealed interface TelemetryState<out T> {
    data object Loading : TelemetryState<Nothing>

    /** [isStale]: the latest poll failed and [data] comes from an earlier one. */
    data class Ready<T>(val data: T, val isStale: Boolean) : TelemetryState<T>

    data class Failed(val failure: TelemetryFailure) : TelemetryState<Nothing>
}
