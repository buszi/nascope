package io.buszi.nascope.domain.dashboard

import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import kotlinx.coroutines.flow.Flow

/** The stored dashboard layout and its changes; [DashboardLayout.Default] when nothing is customized. */
fun interface ObserveDashboardLayoutUseCase {
    operator fun invoke(): Flow<DashboardLayout>
}
