package io.buszi.nascope.ui.dashboard

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.dashboard.ObserveDashboardLayoutUseCase
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
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
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsBrick
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val brickRegistry = mockk<BrickRegistry>()
    private val observeSession = mockk<ObserveSessionUseCase>()
    private val observeConnectionStatus = mockk<ObserveConnectionStatusUseCase>()
    private val observeDashboardLayout = mockk<ObserveDashboardLayoutUseCase>()
    private val observeAvailablePlugins = mockk<ObserveAvailablePluginsUseCase>()
    private val observeAlerts = mockk<ObserveAlertsUseCase>()
    private val alertStates = MutableStateFlow<TelemetryState<ObserveAlertsUseCase.Data>>(TelemetryState.Loading)
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val plugins = MutableStateFlow<Set<String>?>(null)
    private val session = MutableStateFlow<Session>(Session.NotConfigured)
    private val status = MutableStateFlow<Status>(Status.Idle)

    /** The stored layout; `null` stands for "not read yet" (the use case emits nothing until then). */
    private val layout = MutableStateFlow<DashboardLayout?>(DashboardLayout.Default)
    private var registeredBricks: List<DashboardBrick> = listOf(SystemOverviewBrick, GpuBrick)

    private val viewModel by lazy {
        every { brickRegistry.allBricks } returns registeredBricks
        every { observeSession() } returns session
        every { observeConnectionStatus() } returns status
        every { observeDashboardLayout() } returns layout.filterNotNull()
        every { observeAvailablePlugins() } returns plugins
        every { observeAlerts() } returns alertStates
        DashboardViewModel(
            brickRegistry = brickRegistry,
            observeSession = observeSession,
            observeConnectionStatus = observeConnectionStatus,
            observeDashboardLayout = observeDashboardLayout,
            observeAvailablePlugins = observeAvailablePlugins,
            observeAlerts = observeAlerts,
            refreshTelemetry = refreshTelemetry,
        )
    }

    @Test
    fun dashboardViewModelExposesTheRegisteredBricksWithTheDefaultLayoutWithoutASession() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(ViewState(bricks = null), ViewState(bricks = listOf(SystemOverviewBrick, GpuBrick))),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun dashboardViewModelShowsNoBrickBeforeTheLayoutArrives() = runTest {
        layout.value = null
        plugins.value = setOf("cpu", "gpu")
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            val beforeLayout = viewStateTurbine.cancelAndConsumeItems()

            assertTrue(beforeLayout.all { it.bricks == null }, "$beforeLayout")
        }
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            layout.value = DashboardLayout(order = listOf(gpu, system), hidden = emptySet())
            runCurrent()

            // The stored layout is the first one shown: no frame with the default order before it.
            assertEquals(
                listOf(null, listOf(GpuBrick, SystemOverviewBrick)),
                viewStateTurbine.cancelAndConsumeItems().map { it.bricks },
            )
        }
    }

    @Test
    fun dashboardViewModelShowsTheBricksInTheLayoutsOrderAndFollowsLayoutChanges() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            layout.value = DashboardLayout(order = listOf(gpu, system), hidden = emptySet())
            runCurrent()
            layout.value = DashboardLayout(order = listOf(gpu, system), hidden = setOf(gpu))
            runCurrent()
            layout.value = DashboardLayout.Default
            runCurrent()

            assertEquals(
                listOf(
                    null,
                    listOf(SystemOverviewBrick, GpuBrick),
                    listOf(GpuBrick, SystemOverviewBrick),
                    listOf(SystemOverviewBrick),
                    listOf(SystemOverviewBrick, GpuBrick),
                ),
                viewStateTurbine.cancelAndConsumeItems().map { it.bricks },
            )
        }
    }

    @Test
    fun dashboardViewModelFiltersACustomLayoutByAvailabilityAndKeepsItsOrder() = runTest {
        registeredBricks = listOf(SystemOverviewBrick, AlertsBrick, GpuBrick)
        layout.value = DashboardLayout(order = listOf(gpu, alerts, system), hidden = emptySet())
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            plugins.value = setOf("cpu", "alert")
            runCurrent()
            plugins.value = setOf("cpu", "gpu")
            runCurrent()

            assertEquals(
                listOf(
                    null,
                    listOf(GpuBrick, AlertsBrick, SystemOverviewBrick),
                    listOf(AlertsBrick, SystemOverviewBrick),
                    listOf(GpuBrick, SystemOverviewBrick),
                ),
                viewStateTurbine.cancelAndConsumeItems().map { it.bricks },
            )
        }
    }

    @Test
    fun dashboardViewModelShowsAnEmptyListWhenEveryBrickIsTurnedOffOrNoneIsLeftAfterAvailability() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            layout.value = DashboardLayout(order = emptyList(), hidden = setOf(system, gpu))
            runCurrent()
            assertEquals(emptyList(), viewStateTurbine.cancelAndConsumeItems().last().bricks)
        }
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            layout.value = DashboardLayout(order = emptyList(), hidden = setOf(system))
            runCurrent()
            plugins.value = setOf("cpu")
            runCurrent()
            assertEquals(
                listOf(emptyList(), listOf(GpuBrick), emptyList()),
                viewStateTurbine.cancelAndConsumeItems().map { it.bricks },
            )
        }
    }

    @Test
    fun dashboardViewModelKeepsTheAlertChipWithTheAlertsBrickTurnedOff() = runTest {
        registeredBricks = listOf(SystemOverviewBrick, AlertsBrick, GpuBrick)
        layout.value = DashboardLayout(order = emptyList(), hidden = setOf(alerts))
        plugins.value = setOf("alert")
        val critical = GlancesAlert("CPU", AlertState.Critical, 1, null, null, null, null, null, emptyList())
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            alertStates.value = TelemetryState.Ready(ObserveAlertsUseCase.Data(listOf(critical), emptyList(), null), false)
            runCurrent()

            val last = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(listOf(SystemOverviewBrick), last.bricks)
            assertEquals(AlertBadgeUi(1, StatusLevel.Critical), last.alertBadge)
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
                    null,
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
            assertEquals(profile.id.value, states.last().activeProfileId)
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
            assertEquals(null, last.activeProfileId)
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
            observeSession()
            observeConnectionStatus()
            observeDashboardLayout()
            observeAvailablePlugins()
            observeAvailablePlugins()
            refreshTelemetry()
        }
    }

    companion object {
        private val system = DashboardBrickKey("system")
        private val alerts = DashboardBrickKey("alerts")
        private val gpu = DashboardBrickKey("gpu")

        private val profile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
    }
}
