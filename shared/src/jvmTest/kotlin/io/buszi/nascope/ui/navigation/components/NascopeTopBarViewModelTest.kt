package io.buszi.nascope.ui.navigation.components

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.Mode
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NascopeTopBarViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeSession = mockk<ObserveSessionUseCase>()
    private val observeConnectionStatus = mockk<ObserveConnectionStatusUseCase>()

    private val viewModel by lazy {
        NascopeTopBarViewModel(
            observeSession = observeSession,
            observeConnectionStatus = observeConnectionStatus,
        )
    }

    @Test
    fun nascopeTopBarViewModelShowsNothingWhileNothingIsConfigured() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.NotConfigured)
            every { observeConnectionStatus() } returns MutableStateFlow(Status.Idle)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(ViewState(mode = Mode.None)),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                observeSession()
                observeConnectionStatus()
            }
        }
    }

    @Test
    fun nascopeTopBarViewModelShowsDemoWithoutAConnectionStatus() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Demo)
            every { observeConnectionStatus() } returns MutableStateFlow(Status.Idle)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(mode = Mode.None),
                    ViewState(mode = Mode.Demo),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun nascopeTopBarViewModelShowsTheNodeNameWithItsLiveStatus() = runTest {
        turbineScope {
            val status = MutableStateFlow<Status>(Status.Connecting)
            every { observeSession() } returns MutableStateFlow(Session.Connected(profile))
            every { observeConnectionStatus() } returns status

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            status.value = Status.Online
            runCurrent()
            status.value = Status.Offline(ConnectionFailure.ConnectionRefused)
            runCurrent()
            status.value = Status.Online
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    serverState(ConnectionStatusUi.Connecting),
                    serverState(ConnectionStatusUi.Online),
                    serverState(ConnectionStatusUi.Offline),
                    serverState(ConnectionStatusUi.Online),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun nascopeTopBarViewModelReadsAnIdleStatusOfAConnectedSessionAsConnecting() = runTest {
        turbineScope {
            every { observeSession() } returns MutableStateFlow(Session.Connected(profile))
            every { observeConnectionStatus() } returns MutableStateFlow(Status.Idle)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                serverState(ConnectionStatusUi.Connecting),
                viewStateTurbine.cancelAndConsumeItems().last(),
            )
        }
    }

    @Test
    fun nascopeTopBarViewModelSwitchesFromServerToDemo() = runTest {
        turbineScope {
            val session = MutableStateFlow<Session>(Session.Connected(profile))
            val status = MutableStateFlow<Status>(Status.Online)
            every { observeSession() } returns session
            every { observeConnectionStatus() } returns status

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            session.value = Session.Demo
            status.value = Status.Idle
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    serverState(ConnectionStatusUi.Online),
                    ViewState(mode = Mode.Demo),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    private fun serverState(status: ConnectionStatusUi) =
        ViewState(mode = Mode.Server, nodeName = "Alpha", connectionStatus = status)

    companion object {
        private val profile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
    }
}
