package io.buszi.nascope.domain.connection.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.storage.AppPreferencesStore
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
class ObserveSessionUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()
    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        ObserveSessionUseCaseImpl(
            profileStore = profileStore,
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    @Suppress("UnusedFlow")
    fun observeSessionUseCaseEmitsNotConfiguredWithoutProfiles() = runTest {
        turbineScope {
            every { preferencesStore.demoMode } returns MutableStateFlow(false)
            every { profileStore.profiles } returns MutableStateFlow(emptyList())
            every { profileStore.activeProfileId } returns MutableStateFlow(null)

            val sessionTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(Session.NotConfigured),
                sessionTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                preferencesStore.demoMode
                profileStore.profiles
                profileStore.activeProfileId
            }
        }
    }

    @Test
    fun observeSessionUseCaseEmitsConnectedWhenActiveProfileExists() = runTest {
        turbineScope {
            every { preferencesStore.demoMode } returns MutableStateFlow(false)
            every { profileStore.profiles } returns MutableStateFlow(listOf(alpha, beta))
            every { profileStore.activeProfileId } returns MutableStateFlow(beta.id)

            val sessionTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(Session.Connected(beta)),
                sessionTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeSessionUseCaseEmitsDemoWhenDemoModeIsOnEvenWithActiveProfile() = runTest {
        turbineScope {
            every { preferencesStore.demoMode } returns MutableStateFlow(true)
            every { profileStore.profiles } returns MutableStateFlow(listOf(alpha))
            every { profileStore.activeProfileId } returns MutableStateFlow(alpha.id)

            val sessionTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(Session.Demo),
                sessionTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeSessionUseCaseEmitsNotConfiguredWhenActiveIdPointsToMissingProfile() = runTest {
        turbineScope {
            every { preferencesStore.demoMode } returns MutableStateFlow(false)
            every { profileStore.profiles } returns MutableStateFlow(listOf(alpha))
            every { profileStore.activeProfileId } returns MutableStateFlow(beta.id)

            val sessionTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(Session.NotConfigured),
                sessionTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeSessionUseCaseFollowsStoreChanges() = runTest {
        turbineScope {
            val demoMode = MutableStateFlow(false)
            val profiles = MutableStateFlow(emptyList<ServerProfile>())
            val activeProfileId = MutableStateFlow<ServerProfileId?>(null)
            every { preferencesStore.demoMode } returns demoMode
            every { profileStore.profiles } returns profiles
            every { profileStore.activeProfileId } returns activeProfileId

            val sessionTurbine = useCase().testIn(this)
            runCurrent()
            profiles.value = listOf(alpha)
            activeProfileId.value = alpha.id
            runCurrent()
            demoMode.value = true
            runCurrent()
            demoMode.value = false
            runCurrent()

            assertEquals(
                listOf(
                    Session.NotConfigured,
                    Session.Connected(alpha),
                    Session.Demo,
                    Session.Connected(alpha),
                ),
                sessionTurbine.cancelAndConsumeItems(),
            )
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
        private val beta = ServerProfile(
            id = ServerProfileId("beta"),
            alias = "Beta",
            address = ServerAddress(ServerProtocol.Https, "beta.local", 8443),
            apiVersion = GlancesApiVersion.V3,
            auth = ServerAuth.Basic("glances"),
        )
    }
}
