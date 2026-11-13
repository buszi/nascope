package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsPasswordMissingUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val secretStore = mockk<SecretStore>()

    private val useCase by lazy {
        IsPasswordMissingUseCaseImpl(
            secretStore = secretStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun isPasswordMissingUseCaseIsTrueWhenNoSecretIsStored() = runTest {
        coEvery { secretStore.get(protectedProfile.id) } returns null

        assertTrue(useCase(protectedProfile))
    }

    @Test
    fun isPasswordMissingUseCaseIsFalseWhenASecretIsStored() = runTest {
        coEvery { secretStore.get(protectedProfile.id) } returns "secret"

        assertFalse(useCase(protectedProfile))
    }

    @Test
    fun isPasswordMissingUseCaseIsFalseForAStoredEmptyPassword() = runTest {
        coEvery { secretStore.get(protectedProfile.id) } returns ""

        assertFalse(useCase(protectedProfile))
    }

    @Test
    fun isPasswordMissingUseCaseIsFalseWithoutAuthAndDoesNotReadTheStore() = runTest {
        assertFalse(useCase(protectedProfile.copy(auth = ServerAuth.None)))
        coVerify(exactly = 0) { secretStore.get(any()) }
    }

    companion object {
        private val protectedProfile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Https, "alpha.local", 8443),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.Basic("glances"),
        )
    }
}
