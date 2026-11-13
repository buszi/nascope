package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.SaveServerProfileUseCase.Result
import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.storage.ServerProfileStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveServerProfileUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()
    private val preferencesStore = mockk<AppPreferencesStore>()
    private val secretStore = mockk<SecretStore>()

    private val useCase by lazy {
        SaveServerProfileUseCaseImpl(
            profileStore = profileStore,
            preferencesStore = preferencesStore,
            secretStore = secretStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    @Suppress("UnusedFlow")
    fun saveServerProfileUseCaseActivatesNewProfileAndTurnsDemoModeOff() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(emptyList())
        coEvery { secretStore.remove(openProfile.id) } just runs
        coEvery { profileStore.upsert(openProfile) } just runs
        coEvery { profileStore.setActiveProfileId(openProfile.id) } just runs
        coEvery { preferencesStore.setDemoMode(false) } just runs

        val result = useCase(openProfile, password = null)

        assertEquals(Result(secretPersisted = true), result)
        coVerifySequence {
            profileStore.profiles
            secretStore.remove(openProfile.id)
            profileStore.upsert(openProfile)
            profileStore.setActiveProfileId(openProfile.id)
            preferencesStore.setDemoMode(false)
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun saveServerProfileUseCaseKeepsActiveSelectionWhenUpdatingExistingProfile() = runTest {
        val renamed = openProfile.copy(alias = "Renamed")
        every { profileStore.profiles } returns MutableStateFlow(listOf(openProfile))
        coEvery { secretStore.remove(openProfile.id) } just runs
        coEvery { profileStore.upsert(renamed) } just runs

        useCase(renamed, password = null)

        coVerifySequence {
            profileStore.profiles
            secretStore.remove(openProfile.id)
            profileStore.upsert(renamed)
        }
        coVerify(exactly = 0) { profileStore.setActiveProfileId(any()) }
        coVerify(exactly = 0) { preferencesStore.setDemoMode(any()) }
    }

    @Test
    @Suppress("UnusedFlow")
    fun saveServerProfileUseCaseStoresPasswordForBasicAuth() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(protectedProfile))
        every { secretStore.isPersistent } returns true
        coEvery { secretStore.put(protectedProfile.id, "secret") } just runs
        coEvery { profileStore.upsert(protectedProfile) } just runs

        val result = useCase(protectedProfile, password = "secret")

        assertEquals(Result(secretPersisted = true), result)
        coVerifySequence {
            profileStore.profiles
            secretStore.put(protectedProfile.id, "secret")
            secretStore.isPersistent
            profileStore.upsert(protectedProfile)
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun saveServerProfileUseCaseKeepsExistingSecretWhenPasswordIsNull() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(protectedProfile))
        every { secretStore.isPersistent } returns true
        coEvery { profileStore.upsert(protectedProfile) } just runs

        useCase(protectedProfile, password = null)

        coVerifySequence {
            profileStore.profiles
            secretStore.isPersistent
            profileStore.upsert(protectedProfile)
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun saveServerProfileUseCaseRemovesSecretWhenAuthIsNone() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(protectedProfile))
        coEvery { secretStore.remove(protectedProfile.id) } just runs
        coEvery { profileStore.upsert(openedUp) } just runs

        val result = useCase(openedUp, password = null)

        assertEquals(Result(secretPersisted = true), result)
        coVerifySequence {
            profileStore.profiles
            secretStore.remove(protectedProfile.id)
            profileStore.upsert(openedUp)
        }
    }

    @Test
    fun saveServerProfileUseCaseReportsSecretsThatOnlyLiveInMemory() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(protectedProfile))
        every { secretStore.isPersistent } returns false
        coEvery { secretStore.put(protectedProfile.id, "secret") } just runs
        coEvery { profileStore.upsert(protectedProfile) } just runs

        val result = useCase(protectedProfile, password = "secret")

        assertEquals(Result(secretPersisted = false), result)
    }

    @Test
    fun saveServerProfileUseCaseKeepsTheTrustedCertificateOfAnHttpsProfile() = runTest {
        val pinned = openProfile.copy(
            address = openProfile.address.copy(protocol = ServerProtocol.Https),
            trustedCertificate = fingerprint,
        )
        every { profileStore.profiles } returns MutableStateFlow(listOf(pinned))
        coEvery { secretStore.remove(pinned.id) } just runs
        coEvery { profileStore.upsert(any()) } just runs

        useCase(pinned, password = null)

        coVerify { profileStore.upsert(pinned) }
    }

    @Test
    fun saveServerProfileUseCaseDropsTheTrustedCertificateOfAnHttpProfile() = runTest {
        val pinned = openProfile.copy(trustedCertificate = fingerprint)
        every { profileStore.profiles } returns MutableStateFlow(listOf(pinned))
        coEvery { secretStore.remove(pinned.id) } just runs
        coEvery { profileStore.upsert(any()) } just runs

        useCase(pinned, password = null)

        coVerify { profileStore.upsert(openProfile) }
    }

    companion object {
        private val fingerprint = CertificateFingerprint.parse("ab".repeat(32))!!
        private val openProfile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
        private val protectedProfile = openProfile.copy(
            id = ServerProfileId("beta"),
            alias = "Beta",
            auth = ServerAuth.Basic("glances"),
        )
        private val openedUp = protectedProfile.copy(auth = ServerAuth.None)
    }
}
