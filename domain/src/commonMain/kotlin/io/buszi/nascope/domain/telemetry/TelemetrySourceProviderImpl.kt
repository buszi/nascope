package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.network.GlancesTelemetryClient
import io.buszi.nascope.storage.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import kotlin.random.Random

/** Emits a new source whenever the session changes, so consumers can restart polling and drop cached data. */
@Factory
internal class TelemetrySourceProviderImpl(
    private val observeSession: ObserveSessionUseCase,
    private val client: GlancesTelemetryClient,
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : TelemetrySourceProvider {

    override val source: Flow<TelemetrySource?> = observeSession()
        .distinctUntilChanged()
        .map { session ->
            when (session) {
                is Session.Connected -> LiveTelemetrySource(session.profile, client, secretStore, dispatcher)
                Session.Demo -> DemoTelemetrySource(Random.Default)
                Session.NotConfigured -> null
            }
        }
}
