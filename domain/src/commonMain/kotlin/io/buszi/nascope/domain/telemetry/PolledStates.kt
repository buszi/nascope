package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/*
 * The primary / secondary rule of the observing use cases, written once:
 * the state follows the primary plugin; a secondary that is loading, failed or unavailable only yields `null`.
 */

private fun <P, D> Polled<P>.toState(build: (P) -> D): TelemetryState<D> = when (this) {
    Polled.Loading -> TelemetryState.Loading
    is Polled.Value -> TelemetryState.Ready(build(value), isStale)
    is Polled.Failed -> TelemetryState.Failed(failure)
}

internal fun <S> Flow<Polled<S>>.valueOrNull(): Flow<S?> = map { (it as? Polled.Value)?.value }

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

internal fun <P, S1, S2, S3, D> Flow<Polled<P>>.toTelemetryState(
    secondary1: Flow<Polled<S1>>,
    secondary2: Flow<Polled<S2>>,
    secondary3: Flow<Polled<S3>>,
    build: (P, S1?, S2?, S3?) -> D,
): Flow<TelemetryState<D>> = combine(
    this,
    secondary1.valueOrNull(),
    secondary2.valueOrNull(),
    secondary3.valueOrNull(),
) { primary, a, b, c -> primary.toState { build(it, a, b, c) } }

internal fun <T, R> Polled<T>.mapValue(transform: (T) -> R): Polled<R> = when (this) {
    Polled.Loading -> Polled.Loading
    is Polled.Value -> Polled.Value(transform(value), isStale)
    is Polled.Failed -> this
}

/**
 * Runs a per-collection history stage over [this]: [onPolled] sees every emission (to append to or clear
 * the buffers its [state] owns) and returns the value to emit. Because [state] is created when collection
 * starts, a restarted loop starts with empty history.
 */
internal fun <T, S, R> Flow<Polled<T>>.withHistory(
    newState: () -> S,
    onPolled: (state: S, polled: Polled<T>) -> Polled<R>,
): Flow<Polled<R>> = kotlinx.coroutines.flow.flow {
    val state = newState()
    collect { emit(onPolled(state, it)) }
}

/** One loop for all collectors: starts with the first, stops with the last, and never replays a stale value. */
internal fun <T> Flow<T>.shareWhileObserved(scope: CoroutineScope, dispatcher: CoroutineDispatcher): SharedFlow<T> =
    flowOn(dispatcher).shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        replay = 1,
    )
