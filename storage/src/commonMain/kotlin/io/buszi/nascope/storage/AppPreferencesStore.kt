package io.buszi.nascope.storage

import kotlinx.coroutines.flow.Flow

interface AppPreferencesStore {
    val demoMode: Flow<Boolean>

    suspend fun setDemoMode(enabled: Boolean)
}
