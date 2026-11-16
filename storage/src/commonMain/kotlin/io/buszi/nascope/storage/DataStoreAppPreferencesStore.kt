package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
internal class DataStoreAppPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesStore {

    override val demoMode: Flow<Boolean> = dataStore.data
        .map { it[DEMO_MODE_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { it[DEMO_MODE_KEY] = enabled }
    }

    override val refreshInterval: Flow<RefreshInterval> = dataStore.data
        .map { preferences ->
            RefreshInterval.entries.firstOrNull { it.name == preferences[REFRESH_INTERVAL_KEY] }
                ?: RefreshInterval.Default
        }
        .distinctUntilChanged()

    override suspend fun setRefreshInterval(interval: RefreshInterval) {
        dataStore.edit { it[REFRESH_INTERVAL_KEY] = interval.name }
    }

    override val dashboardLayout: Flow<DashboardLayout> = dataStore.data
        .map { decodeLayout(it[DASHBOARD_LAYOUT_KEY]) }
        .distinctUntilChanged()

    override suspend fun setDashboardLayout(layout: DashboardLayout) {
        // Order and visibility go into one value in one edit, so they can never disagree after a crash.
        dataStore.edit { preferences ->
            if (layout == DashboardLayout.Default) {
                preferences.remove(DASHBOARD_LAYOUT_KEY)
            } else {
                preferences[DASHBOARD_LAYOUT_KEY] =
                    LayoutJson.encodeToString(DashboardLayoutDto.serializer(), DashboardLayoutDto.from(layout))
            }
        }
    }

    private fun decodeLayout(json: String?): DashboardLayout {
        if (json == null) return DashboardLayout.Default
        return try {
            LayoutJson.decodeFromString(DashboardLayoutDto.serializer(), json).toDomain()
        } catch (_: IllegalArgumentException) {
            DashboardLayout.Default
        }
    }

    private companion object {
        val DEMO_MODE_KEY = booleanPreferencesKey("demo_mode")
        val REFRESH_INTERVAL_KEY = stringPreferencesKey("refresh_interval")
        val DASHBOARD_LAYOUT_KEY = stringPreferencesKey("dashboard_layout")

        // A layout written by a newer version may carry fields this one does not know; they are skipped.
        val LayoutJson = Json { ignoreUnknownKeys = true }
    }
}
