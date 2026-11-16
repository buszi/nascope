package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.Flow

interface AppPreferencesStore {
    val demoMode: Flow<Boolean>

    suspend fun setDemoMode(enabled: Boolean)

    /** The stored refresh interval; an unknown or missing value reads as [RefreshInterval.Default]. */
    val refreshInterval: Flow<RefreshInterval>

    suspend fun setRefreshInterval(interval: RefreshInterval)

    /** The stored layout; missing or unreadable data reads as [DashboardLayout.Default]. */
    val dashboardLayout: Flow<DashboardLayout>

    /** Stores [layout] in one write; storing [DashboardLayout.Default] removes it. */
    suspend fun setDashboardLayout(layout: DashboardLayout)
}
