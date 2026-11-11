package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreServerProfileStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "test.preferences_pb") }

    @Test
    fun startsEmpty() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreServerProfileStore(dataStore.dataStore)

        assertEquals(emptyList(), store.profiles.first())
        assertNull(store.activeProfileId.first())
        dataStore.close()
    }

    @Test
    fun upsertInsertsThenUpdatesInPlace() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreServerProfileStore(dataStore.dataStore)

        store.upsert(PROFILE_A)
        store.upsert(PROFILE_B)
        assertEquals(listOf(PROFILE_A, PROFILE_B), store.profiles.first())

        val renamed = PROFILE_A.copy(alias = "Renamed", auth = ServerAuth.None)
        store.upsert(renamed)
        assertEquals(listOf(renamed, PROFILE_B), store.profiles.first())
        dataStore.close()
    }

    @Test
    fun removeDeletesOnlyThatProfile() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreServerProfileStore(dataStore.dataStore)
        store.upsert(PROFILE_A)
        store.upsert(PROFILE_B)

        store.remove(PROFILE_A.id)

        assertEquals(listOf(PROFILE_B), store.profiles.first())
        dataStore.close()
    }

    @Test
    fun activeProfileIdRoundTrips() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreServerProfileStore(dataStore.dataStore)

        store.setActiveProfileId(PROFILE_B.id)
        assertEquals(PROFILE_B.id, store.activeProfileId.first())

        store.setActiveProfileId(null)
        assertNull(store.activeProfileId.first())
        dataStore.close()
    }

    @Test
    fun dataSurvivesReopeningTheFile() = runTest {
        val first = DataStoreTestFile(file)
        val firstStore = DataStoreServerProfileStore(first.dataStore)
        firstStore.upsert(PROFILE_A)
        firstStore.upsert(PROFILE_B)
        firstStore.setActiveProfileId(PROFILE_B.id)
        first.close()

        val second = DataStoreTestFile(file)
        val secondStore = DataStoreServerProfileStore(second.dataStore)

        assertEquals(listOf(PROFILE_A, PROFILE_B), secondStore.profiles.first())
        assertEquals(PROFILE_B.id, secondStore.activeProfileId.first())
        second.close()
    }

    private companion object {
        val PROFILE_A = ServerProfile(
            id = ServerProfileId("a"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.Basic("admin"),
        )
        val PROFILE_B = ServerProfile(
            id = ServerProfileId("b"),
            alias = "Beta",
            address = ServerAddress(ServerProtocol.Https, "10.0.0.2", 8443),
            apiVersion = GlancesApiVersion.V3,
            auth = ServerAuth.None,
        )
    }
}
