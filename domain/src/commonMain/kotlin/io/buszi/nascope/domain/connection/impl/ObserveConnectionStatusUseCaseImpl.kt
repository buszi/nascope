package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.connection.toServerConnection
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.network.GlancesConnectionClient
import io.buszi.nascope.storage.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import org.koin.core.annotation.Single

/**
 * Follows the session and, while a server is connected, checks it every [PollIntervalMillis] ms.
 *
 * A single polling loop serves all collectors. It starts with the first collector, stops when the last
 * one leaves, and restarts from `Connecting` when the connected profile changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Single
internal class ObserveConnectionStatusUseCaseImpl(
    observeSession: ObserveSessionUseCase,
    private val client: GlancesConnectionClient,
    private val secretStore: SecretStore,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveConnectionStatusUseCase {

    private val status: Flow<Status> = observeSession()
        .flatMapLatest { session ->
            when (session) {
                is Session.Connected -> poll(session.profile)
                Session.NotConfigured, Session.Demo -> flowOf(Status.Idle)
            }
        }
        .distinctUntilChanged()
        .flowOn(dispatcher)
        .shareIn(
            scope = applicationScope,
            // The replayed status is dropped when the loop stops, so a new collector never sees a stale one.
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            replay = 1,
        )

    override fun invoke(): Flow<Status> = status

    private fun poll(profile: ServerProfile): Flow<Status> = flow {
        emit(Status.Connecting)
        while (true) {
            val connection = profile.toServerConnection(secretStore)
            val failure = client.verifyAccess(connection.address, connection.apiVersion, connection.credentials)
            emit(if (failure == null) Status.Online else Status.Offline(failure))
            delay(PollIntervalMillis)
        }
    }

    private companion object {
        const val PollIntervalMillis = 5_000L
    }
}
