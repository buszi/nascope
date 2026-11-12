package io.buszi.nascope.ui.dashboard.widgets.network

import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewAction.OnToggleInterface
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewState
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
class NetworkDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeNetwork = mockk<ObserveNetworkUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeNetwork() } returns states
        NetworkDetailsViewModel(observeNetwork = observeNetwork, refreshTelemetry = refreshTelemetry)
    }

    private fun emit(vararg interfaces: Interface, stale: Boolean = false) {
        states.value = TelemetryState.Ready(Data(interfaces.toList()), stale)
    }

    @Test
    fun networkDetailsViewModelStartsLoadingThenShowsInterfacesWithoutLoopbackAndKnownSpeeds() = runTest {
        viewModel // created now, so it collects what is emitted below
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            emit(iface("lo"), iface("eth0", rx = 12_500_000, speed = 1_000_000_000), iface("eth1", rx = 1))
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    ViewState(
                        interfaces = listOf(ui("eth0", rx = 12_500_000, speedMbps = 1000), ui("eth1", rx = 1)),
                        widgetState = WidgetState.Ready,
                    ),
                ),
                turbine.cancelAndConsumeItems(),
            )
        }
        assertEquals(0.1f, viewModel.viewState.value.interfaces.first().linkUtilization)
        assertEquals(null, viewModel.viewState.value.interfaces.last().linkUtilization)
    }

    @Test
    fun networkDetailsViewModelTogglesCollapseAndForgetsAnInterfaceThatDisappears() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0"), iface("eth1"))
        runCurrent()

        viewModel.onViewAction(OnToggleInterface("eth1"))
        assertEquals(false, viewModel.viewState.value.isExpanded("eth1"))
        viewModel.onViewAction(OnToggleInterface("eth1"))
        assertEquals(true, viewModel.viewState.value.isExpanded("eth1"))

        viewModel.onViewAction(OnToggleInterface("eth1"))
        emit(iface("eth0"))
        runCurrent()
        assertEquals(emptySet(), viewModel.viewState.value.collapsedNames)
    }

    @Test
    fun networkDetailsViewModelMarksStaleKeepsTheContentAndMapsFailures() = runTest {
        viewModel // created now, so it collects what is emitted below
        emit(iface("eth0"), stale = true)
        runCurrent()
        assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)
        assertEquals(1, viewModel.viewState.value.interfaces.size)

        states.value = TelemetryState.Loading
        runCurrent()
        assertEquals(ViewState(), viewModel.viewState.value)
    }

    @Test
    fun networkDetailsViewStateComputesAggregatesOverActiveInterfaces() {
        val viewState = ViewState(
            interfaces = listOf(ui("eth0", rx = 10, tx = 1), ui("eth1", rx = 5, tx = 2), ui("eth2", isUp = false, rx = 100)),
        )

        assertEquals(15L, viewState.aggregateRxBytesPerSec)
        assertEquals(3L, viewState.aggregateTxBytesPerSec)
        assertEquals(1_150L, viewState.totalRxBytes)
        assertEquals(30L, viewState.totalTxBytes)
        assertEquals(StatusLevel.Operational, viewState.overallStatus)
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
