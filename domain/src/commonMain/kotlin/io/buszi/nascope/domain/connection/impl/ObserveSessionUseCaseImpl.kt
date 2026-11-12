package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.storage.ServerProfileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory
internal class ObserveSessionUseCaseImpl(
    private val profileStore: ServerProfileStore,
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : ObserveSessionUseCase {

    override fun invoke(): Flow<ObserveSessionUseCase.Session> = combine(
        preferencesStore.demoMode,
        profileStore.profiles,
        profileStore.activeProfileId,
    ) { demoMode, profiles, activeId ->
        val active = profiles.firstOrNull { it.id == activeId }
        when {
            demoMode -> ObserveSessionUseCase.Session.Demo
            active != null -> ObserveSessionUseCase.Session.Connected(active)
            else -> ObserveSessionUseCase.Session.NotConfigured
        }
    }.distinctUntilChanged().flowOn(dispatcher)
}
