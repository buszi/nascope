package io.buszi.nascope.domain.connection.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.foreground.AppForegroundState
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.network.GlancesConnectionClient
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveConnectionStatusUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeSession = mockk<ObserveSessionUseCase>()
    private val client = mockk<GlancesConnectionClient>()
    private val secretStore = mockk<SecretStore>()
    private val appForegroundState = FakeAppForegroundState(initial = true)
    private val applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.dispatcher)

    private val useCase by lazy {
        ObserveConnectionStatusUseCaseImpl(
            observeSession = observeSession,
            client = client,
            secretStore = secretStore,
            appForegroundState = appForegroundState,
            applicationScope = applicationScope,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun observeConnectionStatusUseCaseIsIdleWithoutRequestsWhenNothingIsConfigured() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.NotConfigured)

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            advanceTimeBy(20_000)
            runCurrent()

            assertEquals(listOf(Status.Idle), statusTurbine.cancelAndConsumeItems())
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseIsIdleWithoutRequestsInDemoMode() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Demo)

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            advanceTimeBy(20_000)
            runCurrent()

            assertEquals(listOf(Status.Idle), statusTurbine.cancelAndConsumeItems())
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseConnectsThenChecksImmediatelyAndEveryFiveSeconds() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(openProfile.address, GlancesApiVersion.V4, null, null) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }

            advanceTimeBy(4_999)
            runCurrent()
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { client.verifyAccess(any(), any(), any(), any()) }

            advanceTimeBy(5_000)
            runCurrent()
            coVerify(exactly = 3) { client.verifyAccess(any(), any(), any(), any()) }
            assertEquals(
                listOf(Status.Connecting, Status.Online),
                statusTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeConnectionStatusUseCaseGoesOfflineAndRecovers() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(openProfile.address, GlancesApiVersion.V4, null, null) } returnsMany listOf(
                null,
                ConnectionFailure.ConnectionRefused,
                ConnectionFailure.ConnectionRefused,
                null,
            )

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            repeat(3) {
                advanceTimeBy(5_000)
                runCurrent()
            }

            assertEquals(
                listOf(
                    Status.Connecting,
                    Status.Online,
                    Status.Offline(ConnectionFailure.ConnectionRefused),
                    Status.Online,
                ),
                statusTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeConnectionStatusUseCaseChecksWithTheStoredCredentials() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(protectedProfile))
            coEvery { secretStore.get(protectedProfile.id) } returns "secret"
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()

            statusTurbine.cancelAndConsumeItems()
            coVerify {
                client.verifyAccess(
                    protectedProfile.address,
                    GlancesApiVersion.V3,
                    ServerCredentials("glances", "secret"),
                    null,
                )
            }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseReportsAMissingPasswordWithoutARequestWhenTheSecretIsGone() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(protectedProfile))
            coEvery { secretStore.get(protectedProfile.id) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(
                listOf(Status.Connecting, Status.Offline(ConnectionFailure.PasswordMissing)),
                statusTurbine.cancelAndConsumeItems(),
            )
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseChecksWithAStoredEmptyPassword() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(protectedProfile))
            coEvery { secretStore.get(protectedProfile.id) } returns ""
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(listOf(Status.Connecting, Status.Online), statusTurbine.cancelAndConsumeItems())
            coVerify {
                client.verifyAccess(protectedProfile.address, GlancesApiVersion.V3, ServerCredentials("glances", ""), null)
            }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseRestartsTheLoopWhenTheProfileChanges() = runTest {
        turbineScope {
            val session = MutableStateFlow<Session>(Session.Connected(openProfile))
            every { observeSession() } returns session
            coEvery { client.verifyAccess(openProfile.address, any(), any(), null) } returns null
            coEvery { client.verifyAccess(otherProfile.address, any(), any(), null) } returns
                ConnectionFailure.Timeout

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            session.value = Session.Connected(otherProfile)
            runCurrent()

            assertEquals(
                listOf(
                    Status.Connecting,
                    Status.Online,
                    Status.Connecting,
                    Status.Offline(ConnectionFailure.Timeout),
                ),
                statusTurbine.cancelAndConsumeItems(),
            )
            coVerify(exactly = 1) { client.verifyAccess(openProfile.address, any(), any(), null) }
            coVerify(exactly = 1) { client.verifyAccess(otherProfile.address, any(), any(), null) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseStopsCheckingWhenDemoModeStarts() = runTest {
        turbineScope {
            val session = MutableStateFlow<Session>(Session.Connected(openProfile))
            every { observeSession() } returns session
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            session.value = Session.Demo
            runCurrent()
            advanceTimeBy(20_000)
            runCurrent()

            assertEquals(
                listOf(Status.Connecting, Status.Online, Status.Idle),
                statusTurbine.cancelAndConsumeItems(),
            )
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseSharesOneLoopBetweenCollectors() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val firstTurbine = useCase().testIn(this)
            val secondTurbine = useCase().testIn(this)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(listOf(Status.Connecting, Status.Online), firstTurbine.cancelAndConsumeItems())
            assertEquals(listOf(Status.Connecting, Status.Online), secondTurbine.cancelAndConsumeItems())
            coVerify(exactly = 2) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseStopsWhenNobodyCollectsAndRestartsFromConnecting() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val firstTurbine = useCase().testIn(this)
            runCurrent()
            firstTurbine.cancelAndConsumeItems()
            advanceTimeBy(30_000)
            runCurrent()
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }

            val secondTurbine = useCase().testIn(this)
            runCurrent()

            assertEquals(listOf(Status.Connecting, Status.Online), secondTurbine.cancelAndConsumeItems())
            coVerify(exactly = 2) { client.verifyAccess(any(), any(), any(), any()) }
        }
    }

    @Test
    fun observeConnectionStatusUseCaseMakesNoRequestInTheBackgroundAndChecksImmediatelyOnReturn() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }

            appForegroundState.flag.value = false
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            coVerify(exactly = 1) { client.verifyAccess(any(), any(), any(), any()) }

            appForegroundState.flag.value = true
            runCurrent()

            coVerify(exactly = 2) { client.verifyAccess(any(), any(), any(), any()) }
            assertEquals(
                listOf(Status.Connecting, Status.Online),
                statusTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun observeConnectionStatusUseCaseWaitsInConnectingUntilTheAppIsInTheForeground() = runTest {
        turbineScope {
            appForegroundState.flag.value = false
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returns null

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            coVerify(exactly = 0) { client.verifyAccess(any(), any(), any(), any()) }

            appForegroundState.flag.value = true
            runCurrent()

            assertEquals(listOf(Status.Connecting, Status.Online), statusTurbine.cancelAndConsumeItems())
        }
    }

    @Test
    fun observeConnectionStatusUseCaseDoesNotRestartFromConnectingWhenReturningToTheForeground() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(openProfile))
            coEvery { client.verifyAccess(any(), any(), any(), any()) } returnsMany listOf(
                null,
                ConnectionFailure.Timeout,
            )

            val statusTurbine = useCase().testIn(this)
            runCurrent()
            appForegroundState.flag.value = false
            runCurrent()
            appForegroundState.flag.value = true
            runCurrent()

            assertEquals(
                listOf(Status.Connecting, Status.Online, Status.Offline(ConnectionFailure.Timeout)),
                statusTurbine.cancelAndConsumeItems(),
            )
        }
    }

    private class FakeAppForegroundState(initial: Boolean) : AppForegroundState {
        val flag = MutableStateFlow(initial)
        override val isForeground: Flow<Boolean> = flag
        override fun set(isForeground: Boolean) {
            flag.value = isForeground
        }
    }

    companion object {
        private val openProfile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
        private val protectedProfile = ServerProfile(
            id = ServerProfileId("beta"),
            alias = "Beta",
            address = ServerAddress(ServerProtocol.Https, "beta.local", 8443),
            apiVersion = GlancesApiVersion.V3,
            auth = ServerAuth.Basic("glances"),
        )
        private val otherProfile = openProfile.copy(
            id = ServerProfileId("gamma"),
            alias = "Gamma",
            address = ServerAddress(ServerProtocol.Http, "gamma.local", 61208),
        )
    }
}
