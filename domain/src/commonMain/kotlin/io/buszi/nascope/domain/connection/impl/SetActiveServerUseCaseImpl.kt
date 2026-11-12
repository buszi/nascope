package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.SetActiveServerUseCase
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class SetActiveServerUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : SetActiveServerUseCase {

    override suspend fun invoke(id: ServerProfileId) = withContext(dispatcher) {
        profileStore.setActiveProfileId(id)
        preferencesStore.setDemoMode(false)
    }
}
