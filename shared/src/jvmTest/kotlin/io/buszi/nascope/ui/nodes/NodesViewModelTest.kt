package io.buszi.nascope.ui.nodes

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.DeleteServerProfileUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveServerProfilesUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.connection.SetActiveServerUseCase
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.common.model.ServerProtocolUi
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnCancelDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnConfirmDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnExitDemo
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnRequestDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnSetActive
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnUseDemo
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NodesViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeServerProfiles = mockk<ObserveServerProfilesUseCase>()
    private val observeSession = mockk<ObserveSessionUseCase>()
    private val observeConnectionStatus = mockk<ObserveConnectionStatusUseCase>()
    private val setActiveServer = mockk<SetActiveServerUseCase>()
    private val deleteServerProfile = mockk<DeleteServerProfileUseCase>()
    private val setDemoMode = mockk<SetDemoModeUseCase>()

    private val profiles = MutableStateFlow(listOf(alpha, beta))
    private val session = MutableStateFlow<Session>(Session.Connected(alpha))
    private val status = MutableStateFlow<Status>(Status.Online)

    private val viewModel by lazy {
        every { observeServerProfiles() } returns profiles
        every { observeSession() } returns session
        every { observeConnectionStatus() } returns status
        NodesViewModel(
            observeServerProfiles = observeServerProfiles,
            observeSession = observeSession,
            observeConnectionStatus = observeConnectionStatus,
            setActiveServer = setActiveServer,
            deleteServerProfile = deleteServerProfile,
            setDemoMode = setDemoMode,
        )
    }

    @Test
    fun nodesViewModelListsStoredProfilesWithTheActiveOneMarkedAndLive() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    ViewState(nodes = listOf(alphaNode.copy(isActive = true, connectionStatus = ConnectionStatusUi.Online), betaNode), isLoading = false),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun nodesViewModelFollowsTheLiveStatusOfTheActiveNode() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            status.value = Status.Offline(io.buszi.nascope.domain.model.connection.ConnectionFailure.ConnectionRefused)
            runCurrent()

            assertEquals(
                ConnectionStatusUi.Offline,
                viewStateTurbine.cancelAndConsumeItems().last().nodes.first().connectionStatus,
            )
        }
    }

    @Test
    fun nodesViewModelMarksNoNodeActiveInDemoMode() = runTest {
        turbineScope {
            session.value = Session.Demo

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                ViewState(nodes = listOf(alphaNode, betaNode), isDemo = true, isLoading = false),
                viewStateTurbine.cancelAndConsumeItems().last(),
            )
        }
    }

    @Test
    fun nodesViewModelSetsTheChosenNodeActive() = runTest {
        turbineScope {
            coEvery { setActiveServer(ServerProfileId("beta")) } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSetActive("beta"))
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            coVerify(exactly = 1) { setActiveServer(ServerProfileId("beta")) }
        }
    }

    @Test
    fun nodesViewModelAsksForConfirmationBeforeDeletingAndCanCancel() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnRequestDelete("beta"))
            runCurrent()
            viewModel.onViewAction(OnCancelDelete)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(listOf(null, "beta", null), states.drop(1).map { it.pendingDeleteId })
            coVerify(exactly = 0) { deleteServerProfile(any()) }
        }
    }

    @Test
    fun nodesViewModelDeletesTheConfirmedNode() = runTest {
        turbineScope {
            coEvery { deleteServerProfile(ServerProfileId("beta")) } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnRequestDelete("beta"))
            runCurrent()
            viewModel.onViewAction(OnConfirmDelete)
            runCurrent()

            assertEquals(null, viewStateTurbine.cancelAndConsumeItems().last().pendingDeleteId)
            coVerify(exactly = 1) { deleteServerProfile(ServerProfileId("beta")) }
        }
    }

    @Test
    fun nodesViewModelIgnoresAConfirmationWithoutARequest() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnConfirmDelete)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            coVerify(exactly = 0) { deleteServerProfile(any()) }
        }
    }

    @Test
    fun nodesViewModelDropsAPendingConfirmationWhenTheNodeDisappears() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnRequestDelete("beta"))
            runCurrent()
            profiles.value = listOf(alpha)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(null, finalState.pendingDeleteId)
            assertEquals(listOf("alpha"), finalState.nodes.map { it.id })
        }
    }

    @Test
    fun nodesViewModelTurnsDemoModeOnAndOff() = runTest {
        turbineScope {
            coEvery { setDemoMode(any()) } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnUseDemo)
            viewModel.onViewAction(OnExitDemo)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            coVerify(exactly = 1) { setDemoMode(true) }
            coVerify(exactly = 1) { setDemoMode(false) }
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

        private val alphaNode = NodeUi(
            id = "alpha",
            alias = "Alpha",
            host = "alpha.local",
            port = 61208,
            protocol = ServerProtocolUi.Http,
        )
        private val betaNode = NodeUi(
            id = "beta",
            alias = "Beta",
            host = "beta.local",
            port = 8443,
            protocol = ServerProtocolUi.Https,
        )
    }
}
