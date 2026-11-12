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
import io.buszi.nascope.ui.common.model.SessionStatusUi
import io.buszi.nascope.ui.navigation.components.NascopeSidebarRailViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NascopeSidebarRailViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val session = MutableStateFlow<Session>(Session.NotConfigured)
    private val status = MutableStateFlow<Status>(Status.Idle)
    private val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk() } returns session }
    private val observeConnectionStatus = mockk<ObserveConnectionStatusUseCase> { every { this@mockk() } returns status }

    private val viewModel by lazy { NascopeSidebarRailViewModel(observeSession, observeConnectionStatus) }

    @Test
    fun railDotFollowsTheConnectionStatusAndIsNeutralInDemoMode() = runTest {
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            session.value = Session.Connected(profile)
            status.value = Status.Online
            runCurrent()
            status.value = Status.Offline(ConnectionFailure.ConnectionRefused)
            runCurrent()
            session.value = Session.Demo
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(SessionStatusUi.None),
                    ViewState(SessionStatusUi.Server(ConnectionStatusUi.Online)),
                    ViewState(SessionStatusUi.Server(ConnectionStatusUi.Offline)),
                    ViewState(SessionStatusUi.Demo),
                ),
                turbine.cancelAndConsumeItems().distinct(),
            )
        }
    }

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
