package io.buszi.nascope.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun dashboardLayoutDefaultsToDefaultWhenNothingIsStored() = runTest {
        val dataStore = DataStoreTestFile(file)
        assertEquals(DashboardLayout.Default, DataStoreAppPreferencesStore(dataStore.dataStore).dashboardLayout.first())
        dataStore.close()
    }

    @Test
    fun dashboardLayoutSurvivesReopeningTheFile() = runTest {
        val first = DataStoreTestFile(file)
        DataStoreAppPreferencesStore(first.dataStore).setDashboardLayout(customLayout)
        first.close()

        val second = DataStoreTestFile(file)
        assertEquals(customLayout, DataStoreAppPreferencesStore(second.dataStore).dashboardLayout.first())
        second.close()
    }

    @Test
    fun malformedDashboardLayoutJsonReadsAsDefault() = runTest {
        assertStoredLayoutReadsAs(DashboardLayout.Default, """{"order":["cpu",""")
    }

    @Test
    fun dashboardLayoutJsonOfTheWrongShapeReadsAsDefault() = runTest {
        assertStoredLayoutReadsAs(DashboardLayout.Default, """["cpu","memory"]""")
        assertStoredLayoutReadsAs(DashboardLayout.Default, """{"order":"cpu","hidden":[]}""")
        assertStoredLayoutReadsAs(DashboardLayout.Default, """{"order":null}""")
        assertStoredLayoutReadsAs(DashboardLayout.Default, "42")
    }

    @Test
    fun unknownDashboardLayoutJsonFieldsAreIgnored() = runTest {
        assertStoredLayoutReadsAs(
            expected = DashboardLayout(order = listOf(key("cpu"), key("storage")), hidden = setOf(key("storage"))),
            json = """{"order":["cpu","storage"],"hidden":["storage"],"spans":{"cpu":2},"version":3}""",
        )
    }

    @Test
    fun aMissingListInTheDashboardLayoutJsonReadsAsEmpty() = runTest {
        assertStoredLayoutReadsAs(
            expected = DashboardLayout(order = emptyList(), hidden = setOf(key("gpu"))),
            json = """{"hidden":["gpu"]}""",
        )
    }

    @Test
    fun storingTheDefaultDashboardLayoutRemovesTheKey() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreAppPreferencesStore(dataStore.dataStore)

        store.setDashboardLayout(customLayout)
        assertNotNull(dataStore.dataStore.data.first()[DASHBOARD_LAYOUT_KEY])

        store.setDashboardLayout(DashboardLayout.Default)

        assertNull(dataStore.dataStore.data.first()[DASHBOARD_LAYOUT_KEY])
        assertEquals(DashboardLayout.Default, store.dashboardLayout.first())
        dataStore.close()
    }

    @Test
    fun aDashboardLayoutWriteLeavesTheOtherPreferencesUntouched() = runTest {
        val dataStore = DataStoreTestFile(file)
        val preferences = DataStoreAppPreferencesStore(dataStore.dataStore)
        val profiles = DataStoreServerProfileStore(dataStore.dataStore)
        preferences.setDemoMode(true)
        preferences.setRefreshInterval(RefreshInterval.S5)
        profiles.setActiveProfileId(ServerProfileId("nas"))

        preferences.setDashboardLayout(customLayout)
        preferences.setDashboardLayout(DashboardLayout.Default)

        assertTrue(preferences.demoMode.first())
        assertEquals(RefreshInterval.S5, preferences.refreshInterval.first())
        assertEquals(ServerProfileId("nas"), profiles.activeProfileId.first())
        dataStore.close()
    }

    private suspend fun assertStoredLayoutReadsAs(expected: DashboardLayout, json: String) {
        val dataStore = DataStoreTestFile(file)
        dataStore.dataStore.edit { it[DASHBOARD_LAYOUT_KEY] = json }

        assertEquals(expected, DataStoreAppPreferencesStore(dataStore.dataStore).dashboardLayout.first(), json)
        dataStore.close()
    }

    private companion object {
        val DASHBOARD_LAYOUT_KEY = stringPreferencesKey("dashboard_layout")

        fun key(value: String) = DashboardBrickKey(value)

        val customLayout = DashboardLayout(
            order = listOf(key("processes"), key("cpu"), key("memory"), key("gpu")),
            hidden = setOf(key("gpu"), key("memory")),
        )
    }
}
