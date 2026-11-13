package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.IsPasswordMissingUseCase
import io.buszi.nascope.domain.connection.toServerConnection
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.storage.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class IsPasswordMissingUseCaseImpl(
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : IsPasswordMissingUseCase {
    // The same rule the connection itself uses, so the editor and the status never disagree.
    override suspend fun invoke(profile: ServerProfile): Boolean =
        withContext(dispatcher) { profile.toServerConnection(secretStore).isErr }
}
