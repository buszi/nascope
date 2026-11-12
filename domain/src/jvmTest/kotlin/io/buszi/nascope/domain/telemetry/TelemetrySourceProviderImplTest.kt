package io.buszi.nascope.domain.telemetry

import app.cash.turbine.test
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.network.GlancesTelemetryClient
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetrySourceProviderImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val session = MutableStateFlow<Session>(Session.NotConfigured)
    private val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk() } returns session }

    private val provider = TelemetrySourceProviderImpl(
        observeSession = observeSession,
        client = mockk<GlancesTelemetryClient>(),
        secretStore = mockk<SecretStore>(),
        dispatcher = coroutineRule.dispatcher,
    )

    private fun profile(alias: String = "nas", id: String = "1") = ServerProfile(
        ServerProfileId(id), alias, ServerAddress(ServerProtocol.Http, "nas.local", 61208),
        GlancesApiVersion.V4, ServerAuth.None,
    )

    @Test
    fun emitsNullDemoAndLiveForTheThreeSessions() = runTest {
        provider.source.test {
            assertNull(awaitItem())
            session.value = Session.Demo
            assertIs<DemoTelemetrySource>(awaitItem())
            session.value = Session.Connected(profile())
            assertIs<LiveTelemetrySource>(awaitItem())
            session.value = Session.NotConfigured
            assertNull(awaitItem())
        }
    }

    @Test
    fun emitsNewSourceWhenTheActiveProfileChanges() = runTest {
        session.value = Session.Connected(profile("a", "1"))
        provider.source.test {
            val first = awaitItem()
            session.value = Session.Connected(profile("b", "2"))
            assertNotSame(first, awaitItem())
        }
    }

    @Test
    fun doesNotReEmitForAnEqualSession() = runTest {
        // A session source that re-emits equal sessions, e.g. after an unrelated profile-list change.
        every { observeSession() } returns kotlinx.coroutines.flow.flowOf(
            Session.Connected(profile()), Session.Connected(profile()),
        )
        val provider = TelemetrySourceProviderImpl(observeSession, mockk(), mockk(), coroutineRule.dispatcher)
        provider.source.test {
            assertIs<LiveTelemetrySource>(awaitItem())
            awaitComplete()
        }
    }
}
