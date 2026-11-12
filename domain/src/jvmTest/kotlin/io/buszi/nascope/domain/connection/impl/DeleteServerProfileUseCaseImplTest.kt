package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.storage.ServerProfileStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class DeleteServerProfileUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()
    private val secretStore = mockk<SecretStore>()

    private val useCase by lazy {
        DeleteServerProfileUseCaseImpl(
            profileStore = profileStore,
            secretStore = secretStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    @Suppress("UnusedFlow")
    fun deleteServerProfileUseCaseRemovesOnlyThatProfileAndItsSecret() = runTest {
        every { profileStore.activeProfileId } returns MutableStateFlow(alpha.id)
        every { profileStore.profiles } returns MutableStateFlow(listOf(alpha, beta))
        coEvery { profileStore.remove(beta.id) } just runs
        coEvery { secretStore.remove(beta.id) } just runs

        useCase(beta.id)

        coVerifySequence {
            profileStore.activeProfileId
            profileStore.profiles
            profileStore.remove(beta.id)
            secretStore.remove(beta.id)
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun deleteServerProfileUseCaseSelectsFirstRemainingProfileWhenActiveOneIsDeleted() = runTest {
        every { profileStore.activeProfileId } returns MutableStateFlow(beta.id)
        every { profileStore.profiles } returns MutableStateFlow(listOf(alpha, beta, gamma))
        coEvery { profileStore.remove(beta.id) } just runs
        coEvery { secretStore.remove(beta.id) } just runs
        coEvery { profileStore.setActiveProfileId(alpha.id) } just runs

        useCase(beta.id)

        coVerifySequence {
            profileStore.activeProfileId
            profileStore.profiles
            profileStore.remove(beta.id)
            secretStore.remove(beta.id)
            profileStore.setActiveProfileId(alpha.id)
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun deleteServerProfileUseCaseClearsActiveProfileWhenLastOneIsDeleted() = runTest {
        every { profileStore.activeProfileId } returns MutableStateFlow(alpha.id)
        every { profileStore.profiles } returns MutableStateFlow(listOf(alpha))
        coEvery { profileStore.remove(alpha.id) } just runs
        coEvery { secretStore.remove(alpha.id) } just runs
        coEvery { profileStore.setActiveProfileId(null) } just runs

        useCase(alpha.id)

        coVerifySequence {
            profileStore.activeProfileId
            profileStore.profiles
            profileStore.remove(alpha.id)
            secretStore.remove(alpha.id)
            profileStore.setActiveProfileId(null)
        }
    }

    companion object {
        private val alpha = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
        private val beta = alpha.copy(id = ServerProfileId("beta"), alias = "Beta")
        private val gamma = alpha.copy(id = ServerProfileId("gamma"), alias = "Gamma")
    }
}
