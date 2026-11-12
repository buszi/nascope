package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.ObserveServerProfilesUseCase
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory
internal class ObserveServerProfilesUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val dispatcher: CoroutineDispatcher,
) : ObserveServerProfilesUseCase {
    override fun invoke(): Flow<List<ServerProfile>> = profileStore.profiles.flowOn(dispatcher)
}
