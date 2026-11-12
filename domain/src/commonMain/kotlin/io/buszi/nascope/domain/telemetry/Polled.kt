package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.TelemetryFailure

/** The state of one polling loop. */
internal sealed interface Polled<out T> {
    data object Loading : Polled<Nothing>

    /** [isStale]: the latest poll failed and [value] is from an earlier one. */
    data class Value<T>(val value: T, val isStale: Boolean) : Polled<T>

    data class Failed(val failure: TelemetryFailure) : Polled<Nothing>
}
