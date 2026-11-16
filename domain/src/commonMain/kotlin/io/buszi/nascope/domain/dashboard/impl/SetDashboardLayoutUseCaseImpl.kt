package io.buszi.nascope.domain.dashboard.impl

import io.buszi.nascope.domain.dashboard.SetDashboardLayoutUseCase
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.storage.AppPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
internal class SetDashboardLayoutUseCaseImpl(
    private val preferencesStore: AppPreferencesStore,
    private val dispatcher: CoroutineDispatcher,
) : SetDashboardLayoutUseCase {

    override suspend fun invoke(layout: DashboardLayout) = withContext(dispatcher) {
        preferencesStore.setDashboardLayout(layout)
    }
}
