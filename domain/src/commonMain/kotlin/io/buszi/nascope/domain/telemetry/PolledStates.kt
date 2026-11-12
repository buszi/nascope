package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/*
 * The primary / secondary rule of the observing use cases, written once:
 * the state follows the primary plugin; a secondary that is loading, failed or unavailable only yields `null`.
 */

private fun <P, D> Polled<P>.toState(build: (P) -> D): TelemetryState<D> = when (this) {
    Polled.Loading -> TelemetryState.Loading
    is Polled.Value -> TelemetryState.Ready(build(value), isStale)
    is Polled.Failed -> TelemetryState.Failed(failure)
}

private fun <S> Flow<Polled<S>>.valueOrNull(): Flow<S?> = map { (it as? Polled.Value)?.value }

internal fun <P, D> Flow<Polled<P>>.toTelemetryState(build: (P) -> D): Flow<TelemetryState<D>> =
    map { it.toState(build) }

internal fun <P, S, D> Flow<Polled<P>>.toTelemetryState(
    secondary: Flow<Polled<S>>,
    build: (P, S?) -> D,
): Flow<TelemetryState<D>> =
    combine(this, secondary.valueOrNull()) { primary, second -> primary.toState { build(it, second) } }

internal fun <P, S1, S2, D> Flow<Polled<P>>.toTelemetryState(
    secondary1: Flow<Polled<S1>>,
    secondary2: Flow<Polled<S2>>,
    build: (P, S1?, S2?) -> D,
): Flow<TelemetryState<D>> = combine(this, secondary1.valueOrNull(), secondary2.valueOrNull()) { primary, a, b ->
    primary.toState { build(it, a, b) }
}
