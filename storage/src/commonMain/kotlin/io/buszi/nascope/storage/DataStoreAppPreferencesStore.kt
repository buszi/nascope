package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    private companion object {
        val DEMO_MODE_KEY = booleanPreferencesKey("demo_mode")
    }
}
