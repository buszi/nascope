package io.buszi.nascope.domain.settings.impl

import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.storage.AppPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory
internal class ObserveRefreshIntervalUseCaseImpl(
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : ObserveRefreshIntervalUseCase {
    override fun invoke(): Flow<RefreshInterval> = preferencesStore.refreshInterval.flowOn(dispatcher)
}
