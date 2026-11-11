package io.buszi.nascope.ui.dashboard.widgets.network

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.NetworkInterfaceData
import io.buszi.nascope.data.telemetry.NetworkIoData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewAction.OnToggleInterface
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewState
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        NetworkDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun networkDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.networkIo } returns MutableStateFlow(networkData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.networkIo
            }
        }
    }

    @Test
    fun networkDetailsViewModelTogglesInterfaceCollapse() = runTest {
        turbineScope {
            every { telemetryStateHolder.networkIo } returns MutableStateFlow(networkData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertTrue(viewModel.viewState.value.isExpanded("eth0"))

            viewModel.onViewAction(OnToggleInterface("eth0"))
            assertFalse(viewModel.viewState.value.isExpanded("eth0"))
            assertTrue(viewModel.viewState.value.isExpanded("wlan0"))

            viewModel.onViewAction(OnToggleInterface("eth0"))
            assertTrue(viewModel.viewState.value.isExpanded("eth0"))

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun networkDetailsViewModelForgetsCollapsedInterfaceThatDisappears() = runTest {
        turbineScope {
            val networkFlow = MutableStateFlow(networkData)
            every { telemetryStateHolder.networkIo } returns networkFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnToggleInterface("wlan0"))

            networkFlow.value = networkData.copy(interfaces = networkData.interfaces.filter { it.name != "wlan0" })
            runCurrent()

            assertEquals(emptySet(), viewModel.viewState.value.collapsedNames)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun networkDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(listOf("eth0"), state.activeInterfaces.map { it.name })
        assertEquals(10_000_000L, state.aggregateRxBytesPerSec)
        assertEquals(2_000_000L, state.aggregateTxBytesPerSec)
        assertEquals(1_300L, state.totalRxBytes)
        assertEquals(350L, state.totalTxBytes)
        assertEquals(StatusLevel.Operational, state.overallStatus)
        assertEquals(StatusLevel.Critical, state.copy(interfaces = emptyList()).overallStatus)
    }

    companion object {
        private val eth0 = NetworkInterfaceData(
            name = "eth0",
            isUp = true,
            ipAddress = "192.168.1.2/24",
            rxBytesPerSec = 10_000_000L,
            txBytesPerSec = 2_000_000L,
            totalRxBytes = 1_000L,
            totalTxBytes = 300L,
            rxHistory = listOf(1f, 2f),
            txHistory = listOf(0.5f, 1f),
            macAddress = "aa:bb:cc:dd:ee:ff",
            mtu = 1500,
            linkSpeedMbps = 1000,
        )

        private val wlan0 = NetworkInterfaceData(
            name = "wlan0",
            isUp = false,
            ipAddress = "Disconnected",
            rxBytesPerSec = 0L,
            txBytesPerSec = 0L,
            totalRxBytes = 300L,
            totalTxBytes = 50L,
        )

        private val networkData = NetworkIoData(
            interfaces = listOf(eth0, wlan0),
            selectedInterfaceName = "eth0",
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            interfaces = networkData.interfaces.map(NetworkInterfaceUi::from),
            widgetState = WidgetState.Ready,
        )
    }
}
