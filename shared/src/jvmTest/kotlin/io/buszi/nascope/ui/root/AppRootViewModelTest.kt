package io.buszi.nascope.ui.root

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.foreground.SetAppForegroundUseCase
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.root.AppRootViewModel.Destination
import io.buszi.nascope.ui.root.AppRootViewModel.ViewAction
import io.buszi.nascope.ui.root.AppRootViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppRootViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeSession = mockk<ObserveSessionUseCase>()
    private val setAppForeground = mockk<SetAppForegroundUseCase>()
    private val sessions = MutableSharedFlow<Session>()

    private val viewModel by lazy {
        AppRootViewModel(
            observeSession = observeSession,
            setAppForeground = setAppForeground,
        )
    }

    @Test
    fun appRootViewModelStartsInLoadingUntilSessionIsKnown() = runTest {
        turbineScope {
            every { observeSession() } returns sessions

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(ViewState(destination = Destination.Loading)),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                observeSession()
            }
        }
    }

    @Test
    fun appRootViewModelMapsEachSessionToItsDestination() = runTest {
        turbineScope {
            every { observeSession() } returns sessions

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            sessions.emit(Session.NotConfigured)
            runCurrent()
            sessions.emit(Session.Demo)
            runCurrent()
            sessions.emit(Session.NotConfigured)
            runCurrent()
            sessions.emit(Session.Connected(profile))
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(destination = Destination.Loading),
                    ViewState(destination = Destination.Setup),
                    ViewState(destination = Destination.Main),
                    ViewState(destination = Destination.Setup),
                    ViewState(destination = Destination.Main),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun appRootViewModelForwardsForegroundChanges() = runTest {
        every { observeSession() } returns sessions
        every { setAppForeground(any()) } just runs

        viewModel.onViewAction(ViewAction.OnForegroundChanged(true))
        viewModel.onViewAction(ViewAction.OnForegroundChanged(false))

        verifySequence {
            observeSession()
            setAppForeground(true)
            setAppForeground(false)
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
