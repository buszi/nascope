package io.buszi.nascope.domain.settings.impl

import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.settings.SetRefreshIntervalUseCase
import io.buszi.nascope.storage.AppPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class SetRefreshIntervalUseCaseImpl(
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : SetRefreshIntervalUseCase {
    override suspend fun invoke(interval: RefreshInterval) =
        withContext(dispatcher) { preferencesStore.setRefreshInterval(interval) }
}
