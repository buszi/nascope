package io.buszi.nascope.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.connection.CertificateFingerprint
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
import kotlin.test.assertFalse
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

    @Test
    fun trustedCertificateRoundTrips() = runTest {
        val dataStore = DataStoreTestFile(file)
        val store = DataStoreServerProfileStore(dataStore.dataStore)
        val pinned = PROFILE_B.copy(trustedCertificate = FINGERPRINT)

        store.upsert(pinned)

        assertEquals(listOf(pinned), store.profiles.first())
        dataStore.close()
    }

    @Test
    fun aProfileWithoutATrustedCertificateIsStoredInThe100Format() = runTest {
        val dataStore = DataStoreTestFile(file)
        DataStoreServerProfileStore(dataStore.dataStore).upsert(PROFILE_B)

        val raw = dataStore.dataStore.data.first()[PROFILES_KEY].orEmpty()

        assertFalse("trustedCertSha256" in raw, raw)
        dataStore.close()
    }

    @Test
    fun readsProfilesWrittenBy100() = runTest {
        val dataStore = DataStoreTestFile(file)
        dataStore.dataStore.edit {
            it[PROFILES_KEY] = """[{"id":"b","alias":"Beta","protocol":"Https","host":"10.0.0.2","port":8443,"apiVersion":"V3"}]"""
        }

        assertEquals(listOf(PROFILE_B), DataStoreServerProfileStore(dataStore.dataStore).profiles.first())
        dataStore.close()
    }

    @Test
    fun ignoresKeysWrittenByANewerVersion() = runTest {
        val dataStore = DataStoreTestFile(file)
        dataStore.dataStore.edit {
            it[PROFILES_KEY] = """[{"id":"b","alias":"Beta","protocol":"Https","host":"10.0.0.2","port":8443,"apiVersion":"V3","fromTheFuture":true}]"""
        }

        assertEquals(listOf(PROFILE_B), DataStoreServerProfileStore(dataStore.dataStore).profiles.first())
        dataStore.close()
    }

    @Test
    fun dropsAnUnreadableTrustedCertificateButKeepsTheProfile() = runTest {
        val dataStore = DataStoreTestFile(file)
        dataStore.dataStore.edit {
            it[PROFILES_KEY] = """[{"id":"b","alias":"Beta","protocol":"Https","host":"10.0.0.2","port":8443,"apiVersion":"V3","trustedCertSha256":"not-a-fingerprint"}]"""
        }

        assertEquals(listOf(PROFILE_B), DataStoreServerProfileStore(dataStore.dataStore).profiles.first())
        dataStore.close()
    }

    private companion object {
        val PROFILES_KEY = stringPreferencesKey("profiles")
        val FINGERPRINT = CertificateFingerprint.parse("37b1bda5ad60020cb08f056765f402f0ac0c9034c8aa2042502279e37600cae2")!!

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
