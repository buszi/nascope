package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.SaveServerProfileUseCase
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class SaveServerProfileUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val preferencesStore: AppPreferencesStore,
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : SaveServerProfileUseCase {

    override suspend fun invoke(
        profile: ServerProfile,
        password: String?,
    ): SaveServerProfileUseCase.Result = withContext(dispatcher) {
        // A certificate only means something over HTTPS; a profile switched to HTTP forgets it.
        val stored = if (profile.address.protocol == ServerProtocol.Http) profile.copy(trustedCertificate = null) else profile
        val isNew = profileStore.profiles.first().none { it.id == profile.id }

        // The secret goes first, so a stored profile never lacks the password it was saved with.
        val secretPersisted = when (profile.auth) {
            is ServerAuth.None -> {
                secretStore.remove(profile.id)
                true
            }

            is ServerAuth.Basic -> {
                if (password != null) secretStore.put(profile.id, password)
                secretStore.isPersistent
            }
        }
        profileStore.upsert(stored)

        if (isNew) {
            profileStore.setActiveProfileId(profile.id)
            preferencesStore.setDemoMode(false)
        }
        SaveServerProfileUseCase.Result(secretPersisted)
    }
}
