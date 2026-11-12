package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.Flow

interface AppPreferencesStore {
    val demoMode: Flow<Boolean>

    suspend fun setDemoMode(enabled: Boolean)

    /** The stored refresh interval; an unknown or missing value reads as [RefreshInterval.Default]. */
    val refreshInterval: Flow<RefreshInterval>

    suspend fun setRefreshInterval(interval: RefreshInterval)
}
