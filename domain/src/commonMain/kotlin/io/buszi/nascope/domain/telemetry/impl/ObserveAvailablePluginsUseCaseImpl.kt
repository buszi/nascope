package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.mapValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import org.koin.core.annotation.Factory

/**
 * Plugins that Glances lists although they may hold nothing, each with a probe: a flow of polls telling whether the
 * plugin returned any entries. Add a plugin here when its brick must not show on a host without data.
 */
internal val ConfigDependentPlugins: Map<String, (TelemetryPoller) -> Flow<Polled<Boolean>>> = mapOf(
    "gpu" to { poller -> poller.gpus.map { polled -> polled.mapValue { it.isNotEmpty() } } },
)

@OptIn(ExperimentalCoroutinesApi::class)
@Factory
internal class ObserveAvailablePluginsUseCaseImpl(
    private val poller: TelemetryPoller,
) : ObserveAvailablePluginsUseCase {

    override fun invoke(): Flow<Set<String>?> = poller.plugins
        .map { (it as? Polled.Value)?.value }
        .distinctUntilChanged()
        .flatMapLatest { listed -> if (listed == null) flowOf(null) else withProbes(listed) }
        .distinctUntilChanged()

    /** Restarted with every new list, which only happens in a new session: the decisions are made once per session. */
    private fun withProbes(listed: Set<String>): Flow<Set<String>> {
        val probes = ConfigDependentPlugins.filterKeys { it in listed }
        if (probes.isEmpty()) return flowOf(listed)
        val decisions = probes.map { (plugin, probe) -> decide(plugin, probe(poller)) }
        return combine(decisions) { results ->
            listed - probes.keys + results.mapNotNull { (plugin, hasEntries) -> plugin.takeIf { hasEntries } }
        }
    }

    /** Hidden until the first successful poll; that poll decides for the rest of the session and ends the polling. */
    private fun decide(plugin: String, probe: Flow<Polled<Boolean>>): Flow<Pair<String, Boolean>> =
        probe.transformWhile { polled ->
            when {
                polled is Polled.Value && !polled.isStale -> {
                    emit(plugin to polled.value)
                    false
                }
                else -> {
                    emit(plugin to false)
                    true
                }
            }
        }
}
