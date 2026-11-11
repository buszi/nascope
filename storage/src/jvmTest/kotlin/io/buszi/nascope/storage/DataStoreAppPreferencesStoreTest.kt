package io.buszi.nascope.storage

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
}
