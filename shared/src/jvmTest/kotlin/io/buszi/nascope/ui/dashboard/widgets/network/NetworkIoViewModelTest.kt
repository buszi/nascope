package io.buszi.nascope.ui.dashboard.widgets.network

import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnSelectInterface
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewState
import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase.Interface
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkIoViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeNetwork = mockk<ObserveNetworkUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeNetwork() } returns states
        NetworkIoViewModel(observeNetwork = observeNetwork, refreshTelemetry = refreshTelemetry)
    }

    private fun emit(vararg interfaces: Interface, stale: Boolean = false) {
        states.value = TelemetryState.Ready(Data(interfaces.toList()), stale)
    }

    @Test
    fun networkIoViewModelStartsLoadingThenShowsActiveInterfacesWithoutLoopback() = runTest {
        viewModel // created now, so it collects what is emitted below
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            emit(iface("lo", rx = 5), iface("eth0", rx = 100, tx = 10), iface("eth1", isUp = false), iface("eth2", isUp = null, rx = 1, tx = 2))
            runCurrent()

            val states = turbine.cancelAndConsumeItems()
            assertEquals(ViewState(), states.first())
            assertEquals(
                ViewState(
                    overallStatus = StatusLevel.Operational,
                    interfaces = listOf(ui("eth0", rx = 100, tx = 10), ui("eth1", isUp = false), ui("eth2", rx = 1, tx = 2)),
                    selectedInterface = ui("eth0", rx = 100, tx = 10),
                    aggregateRxBytesPerSec = 101,
                    aggregateTxBytesPerSec = 12,
                    activeInterfacesCount = 2,
                    widgetState = WidgetState.Ready,
                ),
                states.last(),
            )
        }
    }

    @Test
    fun networkIoViewModelKeepsTheSelectionAcrossUpdatesAndFallsBackWhenItVanishes() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0", rx = 1), iface("eth1", rx = 2))
        runCurrent()

        viewModel.onViewAction(OnSelectInterface("eth1"))
        emit(iface("eth0", rx = 3), iface("eth1", rx = 4))
        runCurrent()
        assertEquals(ui("eth1", rx = 4), viewModel.viewState.value.selectedInterface)

        emit(iface("eth0", rx = 5))
        runCurrent()
        assertEquals(ui("eth0", rx = 5), viewModel.viewState.value.selectedInterface)
    }

    @Test
    fun networkIoViewModelIgnoresSelectingAnUnknownInterface() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0"))
        runCurrent()

        viewModel.onViewAction(OnSelectInterface("nope"))

        assertEquals("eth0", viewModel.viewState.value.selectedInterface?.name)
    }

    @Test
    fun networkIoViewModelReportsCriticalWhenEverythingIsDown() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0", isUp = false))
        runCurrent()

        assertEquals(StatusLevel.Critical, viewModel.viewState.value.overallStatus)
        assertEquals(0, viewModel.viewState.value.activeInterfacesCount)
    }

    @Test
    fun networkIoViewModelShowsAnEmptyInterfaceListAsReadyAndCritical() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("lo"))
        runCurrent()

        assertEquals(ViewState(overallStatus = StatusLevel.Critical, widgetState = WidgetState.Ready), viewModel.viewState.value)
    }

    @Test
    fun networkIoViewModelMarksStaleAndMapsFailures() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0"), stale = true)
        runCurrent()
        assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout))
        runCurrent()
        assertIs<WidgetState.Offline>(viewModel.viewState.value.widgetState)
        assertEquals(1, viewModel.viewState.value.interfaces.size)

        states.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Loading
        runCurrent()
        assertEquals(ViewState(), viewModel.viewState.value)
    }

    @Test
    fun networkIoViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeNetwork()
            refreshTelemetry()
        }
    }

    companion object {
        private fun iface(name: String, isUp: Boolean? = true, rx: Long = 0, tx: Long = 0, speed: Long? = null) = Interface(
            usage = NetworkInterfaceUsage(name, null, isUp, speed, rx, tx, rx * 10, tx * 10),
            rxHistory = listOf(rx.toFloat()),
            txHistory = listOf(tx.toFloat()),
        )

        private fun ui(name: String, isUp: Boolean = true, rx: Long = 0, tx: Long = 0, speedMbps: Int? = null) = NetworkInterfaceUi(
            name = name, isUp = isUp, rxBytesPerSec = rx, txBytesPerSec = tx, totalRxBytes = rx * 10, totalTxBytes = tx * 10,
            rxHistory = listOf(rx.toFloat()), txHistory = listOf(tx.toFloat()), linkSpeedMbps = speedMbps,
        )
    }
}
