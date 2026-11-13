package io.buszi.nascope.domain.connection.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.TestConnectionUseCase.Event
import io.buszi.nascope.domain.connection.TestConnectionUseCase.Step
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.network.GlancesConnectionClient
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.awaitItemsUntilComplete
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class TestConnectionUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val client = mockk<GlancesConnectionClient>()
    private val secretStore = mockk<SecretStore>()

    private val useCase by lazy {
        TestConnectionUseCaseImpl(
            client = client,
            secretStore = secretStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun testConnectionUseCaseSucceedsForGlances4() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, null) } returns null

            val eventTurbine = useCase(address, credentials = null, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + authenticatePassed + Event.Succeeded(GlancesApiVersion.V4),
                eventTurbine.awaitItemsUntilComplete(),
            )
            coVerifySequence {
                client.fetchStatus(address, GlancesApiVersion.V4)
                client.verifyAccess(address, GlancesApiVersion.V4, null)
            }
        }
    }

    @Test
    fun testConnectionUseCaseFallsBackToGlances3() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns ConnectionFailure.NotGlances
            coEvery { client.fetchStatus(address, GlancesApiVersion.V3) } returns null
            coEvery { client.verifyAccess(address, GlancesApiVersion.V3, null) } returns null

            val eventTurbine = useCase(address, credentials = null, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + authenticatePassed + Event.Succeeded(GlancesApiVersion.V3),
                eventTurbine.awaitItemsUntilComplete(),
            )
            coVerifySequence {
                client.fetchStatus(address, GlancesApiVersion.V4)
                client.fetchStatus(address, GlancesApiVersion.V3)
                client.verifyAccess(address, GlancesApiVersion.V3, null)
            }
        }
    }

    @Test
    fun testConnectionUseCaseTreatsUnauthorizedStatusAsProtectedGlances3() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns ConnectionFailure.Unauthorized
            coEvery { client.fetchStatus(address, GlancesApiVersion.V3) } returns ConnectionFailure.Unauthorized
            coEvery { client.verifyAccess(address, GlancesApiVersion.V3, credentials) } returns null

            val eventTurbine = useCase(address, credentials, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + authenticatePassed + Event.Succeeded(GlancesApiVersion.V3),
                eventTurbine.awaitItemsUntilComplete(),
            )
        }
    }

    @Test
    fun testConnectionUseCaseFailsReachHostOnEveryTransportFailure() = runTest {
        turbineScope {
            transportFailures.forEach { failure ->
                coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns failure

                val eventTurbine = useCase(address, credentials = null, existingProfileId = null).testIn(this)
                runCurrent()

                assertEquals(
                    listOf(
                        Event.StepStarted(Step.ReachHost),
                        Event.Failed(Step.ReachHost, failure),
                    ),
                    eventTurbine.awaitItemsUntilComplete(),
                )
            }
            coVerify(exactly = transportFailures.size) { client.fetchStatus(address, GlancesApiVersion.V4) }
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any()) }
        }
    }

    @Test
    fun testConnectionUseCaseFailsDetectGlancesWhenNeitherVersionAnswers() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns ConnectionFailure.NotGlances
            coEvery { client.fetchStatus(address, GlancesApiVersion.V3) } returns ConnectionFailure.NotGlances

            val eventTurbine = useCase(address, credentials = null, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + listOf(
                    Event.StepStarted(Step.DetectGlances),
                    Event.Failed(Step.DetectGlances, ConnectionFailure.NotGlances),
                ),
                eventTurbine.awaitItemsUntilComplete(),
            )
        }
    }

    @Test
    fun testConnectionUseCaseReportsTransportFailureOfTheGlances3FallbackAsDetectFailure() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns ConnectionFailure.NotGlances
            coEvery { client.fetchStatus(address, GlancesApiVersion.V3) } returns ConnectionFailure.Timeout

            val eventTurbine = useCase(address, credentials = null, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + listOf(
                    Event.StepStarted(Step.DetectGlances),
                    Event.Failed(Step.DetectGlances, ConnectionFailure.Timeout),
                ),
                eventTurbine.awaitItemsUntilComplete(),
            )
        }
    }

    @Test
    fun testConnectionUseCaseFailsAuthenticateWhenCredentialsAreRejected() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, credentials) } returns
                ConnectionFailure.Unauthorized

            val eventTurbine = useCase(address, credentials, existingProfileId = null).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + listOf(
                    Event.StepStarted(Step.Authenticate),
                    Event.Failed(Step.Authenticate, ConnectionFailure.Unauthorized),
                ),
                eventTurbine.awaitItemsUntilComplete(),
            )
        }
    }

    @Test
    fun testConnectionUseCaseUsesTypedPasswordAsIs() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, credentials) } returns null

            val eventTurbine = useCase(address, credentials, existingProfileId).testIn(this)
            runCurrent()

            eventTurbine.awaitItemsUntilComplete()
            coVerify(exactly = 0) { secretStore.get(any()) }
            coVerify { client.verifyAccess(address, GlancesApiVersion.V4, credentials) }
        }
    }

    @Test
    fun testConnectionUseCaseUsesStoredSecretWhenPasswordIsEmptyForExistingProfile() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { secretStore.get(existingProfileId) } returns "stored"
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, credentials.copy(password = "stored")) } returns null

            val eventTurbine = useCase(address, credentials.copy(password = ""), existingProfileId).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + authenticatePassed + Event.Succeeded(GlancesApiVersion.V4),
                eventTurbine.awaitItemsUntilComplete(),
            )
            coVerify { secretStore.get(existingProfileId) }
        }
    }

    @Test
    fun testConnectionUseCaseFailsAuthenticateWithPasswordMissingWhenExistingProfileHasNoStoredSecret() = runTest {
        turbineScope {
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { secretStore.get(existingProfileId) } returns null

            val eventTurbine = useCase(address, credentials.copy(password = ""), existingProfileId).testIn(this)
            runCurrent()

            assertEquals(
                reachHostPassed + detectGlancesPassed + listOf(
                    Event.StepStarted(Step.Authenticate),
                    Event.Failed(Step.Authenticate, ConnectionFailure.PasswordMissing),
                ),
                eventTurbine.awaitItemsUntilComplete(),
            )
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any()) }
        }
    }

    @Test
    fun testConnectionUseCaseUsesAStoredEmptyPasswordForExistingProfile() = runTest {
        turbineScope {
            val emptyPassword = credentials.copy(password = "")
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { secretStore.get(existingProfileId) } returns ""
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, emptyPassword) } returns null

            val eventTurbine = useCase(address, emptyPassword, existingProfileId).testIn(this)
            runCurrent()

            assertEquals(Event.Succeeded(GlancesApiVersion.V4), eventTurbine.awaitItemsUntilComplete().last())
        }
    }

    @Test
    fun testConnectionUseCaseDoesNotLookUpSecretForNewProfile() = runTest {
        turbineScope {
            val emptyPassword = credentials.copy(password = "")
            coEvery { client.fetchStatus(address, GlancesApiVersion.V4) } returns null
            coEvery { client.verifyAccess(address, GlancesApiVersion.V4, emptyPassword) } returns null

            val eventTurbine = useCase(address, emptyPassword, existingProfileId = null).testIn(this)
            runCurrent()

            eventTurbine.awaitItemsUntilComplete()
            coVerify(exactly = 0) { secretStore.get(any()) }
        }
    }

    companion object {
        private val address = ServerAddress(ServerProtocol.Http, "nas.local", 61208)
        private val credentials = ServerCredentials(username = "glances", password = "typed")
        private val existingProfileId = ServerProfileId("existing")

        private val transportFailures = listOf(
            ConnectionFailure.UnknownHost,
            ConnectionFailure.ConnectionRefused,
            ConnectionFailure.Timeout,
            ConnectionFailure.TlsError,
        )

        private val reachHostPassed = listOf(
            Event.StepStarted(Step.ReachHost),
            Event.StepPassed(Step.ReachHost),
        )
        private val detectGlancesPassed = listOf(
            Event.StepStarted(Step.DetectGlances),
            Event.StepPassed(Step.DetectGlances),
        )
        private val authenticatePassed = listOf(
            Event.StepStarted(Step.Authenticate),
            Event.StepPassed(Step.Authenticate),
        )
    }
}
