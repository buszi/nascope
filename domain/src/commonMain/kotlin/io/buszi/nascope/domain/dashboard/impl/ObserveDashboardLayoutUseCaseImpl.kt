package io.buszi.nascope.domain.dashboard.impl

import io.buszi.nascope.domain.dashboard.ObserveDashboardLayoutUseCase
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.storage.AppPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory
internal class ObserveDashboardLayoutUseCaseImpl(
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : ObserveDashboardLayoutUseCase {

    override fun invoke(): Flow<DashboardLayout> =
        preferencesStore.dashboardLayout.flowOn(dispatcher)
}
