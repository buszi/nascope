package io.buszi.nascope.domain.connection.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.ServerProfileStore
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveServerProfilesUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()

    private val useCase by lazy {
        ObserveServerProfilesUseCaseImpl(
            profileStore = profileStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    @Suppress("UnusedFlow")
    fun observeServerProfilesUseCaseEmitsStoredProfilesAndUpdates() = runTest {
        turbineScope {
            val profiles = MutableStateFlow(listOf(alpha))
            every { profileStore.profiles } returns profiles

            val profilesTurbine = useCase().testIn(this)
            runCurrent()
            profiles.value = listOf(alpha, beta)
            runCurrent()

            assertEquals(
                listOf(
                    listOf(alpha),
                    listOf(alpha, beta),
                ),
                profilesTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                profileStore.profiles
            }
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
    }
}
