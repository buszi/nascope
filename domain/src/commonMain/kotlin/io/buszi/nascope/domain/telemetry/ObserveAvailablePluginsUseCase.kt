package io.buszi.nascope.domain.telemetry

import kotlinx.coroutines.flow.Flow

/**
 * The Glances plugins the connected server can serve, from its `pluginslist`. `null` while that list is loading or
 * has failed (unknown): callers must then assume that everything is available.
 *
 * A plugin that is listed but may hold nothing (see [ConfigDependentPlugins]) is included only after its first
 * successful poll returned entries. That is decided once per session, so a brick does not flap.
 */
fun interface ObserveAvailablePluginsUseCase {
    operator fun invoke(): Flow<Set<String>?>
}
