package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.storage.AppPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class SetDemoModeUseCaseImpl(
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : SetDemoModeUseCase {
    override suspend fun invoke(enabled: Boolean) = withContext(dispatcher) { preferencesStore.setDemoMode(enabled) }
}
