package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.DeleteServerProfileUseCase
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class DeleteServerProfileUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : DeleteServerProfileUseCase {

    override suspend fun invoke(id: ServerProfileId) = withContext(dispatcher) {
        val wasActive = profileStore.activeProfileId.first() == id
        val remaining = profileStore.profiles.first().filterNot { it.id == id }

        profileStore.remove(id)
        secretStore.remove(id)

        if (wasActive) profileStore.setActiveProfileId(remaining.firstOrNull()?.id)
    }
}
