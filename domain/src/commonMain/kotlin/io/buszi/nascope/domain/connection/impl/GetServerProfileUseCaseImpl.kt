package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.GetServerProfileUseCase
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class GetServerProfileUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val dispatcher: CoroutineDispatcher,
) : GetServerProfileUseCase {
    override suspend fun invoke(id: ServerProfileId): ServerProfile? =
        withContext(dispatcher) { profileStore.profiles.first().firstOrNull { it.id == id } }
}
