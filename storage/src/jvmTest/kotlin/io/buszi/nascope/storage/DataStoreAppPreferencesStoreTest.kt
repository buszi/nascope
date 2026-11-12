package io.buszi.nascope.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreAppPreferencesStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "test.preferences_pb") }

    @Test
    fun demoModeDefaultsToFalse() = runTest {
        val dataStore = DataStoreTestFile(file)
        assertFalse(DataStoreAppPreferencesStore(dataStore.dataStore).demoMode.first())
        dataStore.close()
    }

    @Test
    fun demoModeSurvivesReopeningTheFile() = runTest {
        val first = DataStoreTestFile(file)
        DataStoreAppPreferencesStore(first.dataStore).setDemoMode(true)
        first.close()

        val second = DataStoreTestFile(file)
        assertTrue(DataStoreAppPreferencesStore(second.dataStore).demoMode.first())
        second.close()
    }

    @Test
    fun sharesTheFileWithProfileStoreWithoutClobbering() = runTest {
        val dataStore = DataStoreTestFile(file)
        val preferences = DataStoreAppPreferencesStore(dataStore.dataStore)
        val profiles = DataStoreServerProfileStore(dataStore.dataStore)

        preferences.setDemoMode(true)
        profiles.setActiveProfileId(null)

        assertTrue(preferences.demoMode.first())
        assertEquals(emptyList(), profiles.profiles.first())
        dataStore.close()
    }

    @Test
    fun refreshIntervalDefaultsToTwoSeconds() = runTest {
        val dataStore = DataStoreTestFile(file)
        assertEquals(RefreshInterval.S2, DataStoreAppPreferencesStore(dataStore.dataStore).refreshInterval.first())
        dataStore.close()
    }

    @Test
    fun refreshIntervalSurvivesReopeningTheFile() = runTest {
        val first = DataStoreTestFile(file)
        DataStoreAppPreferencesStore(first.dataStore).setRefreshInterval(RefreshInterval.S10)
        first.close()

        val second = DataStoreTestFile(file)
        assertEquals(RefreshInterval.S10, DataStoreAppPreferencesStore(second.dataStore).refreshInterval.first())
        second.close()
    }

    @Test
    fun anUnknownStoredRefreshIntervalFallsBackToTheDefault() = runTest {
        val dataStore = DataStoreTestFile(file)
        dataStore.dataStore.edit { it[stringPreferencesKey("refresh_interval")] = "S7" }

        assertEquals(RefreshInterval.S2, DataStoreAppPreferencesStore(dataStore.dataStore).refreshInterval.first())
        dataStore.close()
    }
}
