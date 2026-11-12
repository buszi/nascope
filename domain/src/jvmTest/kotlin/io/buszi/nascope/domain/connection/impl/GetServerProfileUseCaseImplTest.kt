package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.ServerProfileStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetServerProfileUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()

    private val useCase by lazy {
        GetServerProfileUseCaseImpl(
            profileStore = profileStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    @Suppress("UnusedFlow")
    fun getServerProfileUseCaseReturnsProfileWithMatchingId() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(alpha, beta))

        assertEquals(beta, useCase(beta.id))
        verifySequence {
            profileStore.profiles
        }
    }

    @Test
    fun getServerProfileUseCaseReturnsNullForUnknownId() = runTest {
        every { profileStore.profiles } returns MutableStateFlow(listOf(alpha))

        assertNull(useCase(beta.id))
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
    }
}
