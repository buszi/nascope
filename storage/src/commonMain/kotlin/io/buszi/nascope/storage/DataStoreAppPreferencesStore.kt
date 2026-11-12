package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

    private companion object {
        val DEMO_MODE_KEY = booleanPreferencesKey("demo_mode")
        val REFRESH_INTERVAL_KEY = stringPreferencesKey("refresh_interval")
    }
}
