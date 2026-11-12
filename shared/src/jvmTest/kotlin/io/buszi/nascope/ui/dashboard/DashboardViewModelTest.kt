package io.buszi.nascope.ui.dashboard

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
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.AlertBadgeUi
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.model.SessionStatusUi
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewAction.OnRefresh
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val brickRegistry = mockk<BrickRegistry>()
    private val observeSession = mockk<ObserveSessionUseCase>()
    private val observeConnectionStatus = mockk<ObserveConnectionStatusUseCase>()
    private val observeAvailablePlugins = mockk<ObserveAvailablePluginsUseCase>()
    private val observeAlerts = mockk<ObserveAlertsUseCase>()
    private val alertStates = MutableStateFlow<TelemetryState<ObserveAlertsUseCase.Data>>(TelemetryState.Loading)
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val plugins = MutableStateFlow<Set<String>?>(null)
    private val session = MutableStateFlow<Session>(Session.NotConfigured)
    private val status = MutableStateFlow<Status>(Status.Idle)

    private val viewModel by lazy {
        every { brickRegistry.getActiveBricks() } returns listOf(SystemOverviewBrick, GpuBrick)
        every { observeSession() } returns session
        every { observeConnectionStatus() } returns status
        every { observeAvailablePlugins() } returns plugins
        every { observeAlerts() } returns alertStates
        DashboardViewModel(
            brickRegistry = brickRegistry,
            observeSession = observeSession,
            observeConnectionStatus = observeConnectionStatus,
            observeAvailablePlugins = observeAvailablePlugins,
            observeAlerts = observeAlerts,
            refreshTelemetry = refreshTelemetry,
        )
    }

    @Test
    fun dashboardViewModelExposesActiveBricksFromRegistryWithoutASession() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(ViewState(bricks = listOf(SystemOverviewBrick, GpuBrick))),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun dashboardViewModelFiltersTheBricksOnceThePluginsAreKnownAndShowsAllAgainWhenTheyAreNot() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            plugins.value = setOf("cpu")
            runCurrent()
            plugins.value = setOf("cpu", "gpu")
            runCurrent()
            plugins.value = null // the server was switched: unknown again
            runCurrent()
            plugins.value = setOf("cpu")
            runCurrent()

            val shown = viewStateTurbine.cancelAndConsumeItems().map { it.bricks }
            assertEquals(
                listOf(
                    listOf(SystemOverviewBrick, GpuBrick),
                    listOf(SystemOverviewBrick),
                    listOf(SystemOverviewBrick, GpuBrick),
                    listOf(SystemOverviewBrick),
                ),
                shown.filterIndexed { i, bricks -> i == 0 || bricks != shown[i - 1] },
            )
        }
    }

    @Test
    fun dashboardViewModelShowsAnAlertBadgeOnlyOnAServerWithTheAlertPlugin() = runTest {
        val warning = GlancesAlert("MEM", AlertState.Warning, 1, null, null, null, null, null, emptyList())
        val critical = warning.copy(type = "CPU", state = AlertState.Critical)
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            alertStates.value = TelemetryState.Ready(ObserveAlertsUseCase.Data(listOf(critical, warning), emptyList(), null), false)
            runCurrent()
            assertEquals(null, turbine.cancelAndConsumeItems().last().alertBadge) // plugins still unknown: not polled
        }
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            plugins.value = setOf("cpu", "alert")
            runCurrent()
            assertEquals(AlertBadgeUi(2, StatusLevel.Critical), turbine.cancelAndConsumeItems().last().alertBadge)
        }
    }

    @Test
    fun dashboardViewModelHidesTheAlertBadgeWithoutAlertsOrWhenTheyCannotBeRead() = runTest {
        plugins.value = setOf("alert")
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            alertStates.value = TelemetryState.Ready(ObserveAlertsUseCase.Data(emptyList(), emptyList(), null), false)
            runCurrent()
            assertEquals(null, turbine.cancelAndConsumeItems().last().alertBadge)
        }
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            alertStates.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()
            assertEquals(null, turbine.cancelAndConsumeItems().last().alertBadge)
        }
    }

    @Test
    fun dashboardViewModelShowsTheActiveAliasAndFollowsTheConnectionStatus() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            session.value = Session.Connected(profile)
            status.value = Status.Connecting
            runCurrent()
            status.value = Status.Online
            runCurrent()
            status.value = Status.Offline(ConnectionFailure.Timeout)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals("Alpha", states.last().nodeName)
            assertEquals(
                listOf(
                    SessionStatusUi.None,
                    SessionStatusUi.Server(ConnectionStatusUi.Connecting),
                    SessionStatusUi.Server(ConnectionStatusUi.Online),
                    SessionStatusUi.Server(ConnectionStatusUi.Offline),
                ),
                states.map { it.sessionStatus }.distinct(),
            )
        }
    }

    @Test
    fun dashboardViewModelShowsADemoStatusWithoutAnAlias() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            session.value = Session.Demo
            runCurrent()

            val last = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals("", last.nodeName)
            assertEquals(SessionStatusUi.Demo, last.sessionStatus)
        }
    }

    @Test
    fun dashboardViewModelSwitchingServersShowsTheNewAlias() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            session.value = Session.Connected(profile)
            runCurrent()
            session.value = Session.Connected(profile.copy(id = ServerProfileId("beta"), alias = "Beta"))
            runCurrent()

            assertEquals("Beta", viewStateTurbine.cancelAndConsumeItems().last().nodeName)
        }
    }

    @Test
    fun dashboardViewModelRefreshActionRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRefresh)

        verifySequence {
            brickRegistry.getActiveBricks()
            observeSession()
            observeConnectionStatus()
            observeAvailablePlugins()
            observeAvailablePlugins()
            refreshTelemetry()
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
