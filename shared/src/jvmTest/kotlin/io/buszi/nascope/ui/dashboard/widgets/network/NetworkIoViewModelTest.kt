package io.buszi.nascope.ui.dashboard.widgets.network

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.NetworkInterfaceData
import io.buszi.nascope.data.telemetry.NetworkIoData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnSelectInterface
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewState
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
class NetworkIoViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        NetworkIoViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun networkIoViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            val networkFlow = MutableStateFlow(testNetworkData)
            every { telemetryStateHolder.networkIo } returns networkFlow

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
    fun selectingInterfaceUpdatesSelectedInterfaceInViewState() = runTest {
        turbineScope {
            val networkFlow = MutableStateFlow(testNetworkData)
            every { telemetryStateHolder.networkIo } returns networkFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnSelectInterface("eth1"))
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(selectedInterface = eth1Interface),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun allInterfacesDownProducesCriticalStatus() = runTest {
        turbineScope {
            val downData = NetworkIoData(
                interfaces = listOf(
                    eth0Interface.copy(isUp = false),
                    eth1Interface.copy(isUp = false),
                ),
            )
            val networkFlow = MutableStateFlow(downData)
            every { telemetryStateHolder.networkIo } returns networkFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(StatusLevel.Critical, states.last().overallStatus)
            assertEquals(0, states.last().activeInterfacesCount)
        }
    }

    @Test
    fun retryInvokesStateHolderRefresh() = runTest {
        turbineScope {
            val networkFlow = MutableStateFlow(testNetworkData)
            every { telemetryStateHolder.networkIo } returns networkFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnRetry)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.networkIo
                telemetryStateHolder.refresh()
            }
        }
    }

    private companion object {
        val eth0Interface = NetworkInterfaceData(
            name = "eth0",
            isUp = true,
            ipAddress = "192.168.1.120/24",
            rxBytesPerSec = 14_850_000L,
            txBytesPerSec = 3_210_000L,
            totalRxBytes = 842_300_000_000L,
            totalTxBytes = 195_600_000_000L,
            rxHistory = listOf(10f, 15f),
            txHistory = listOf(2f, 3f),
        )

        val eth1Interface = NetworkInterfaceData(
            name = "eth1",
            isUp = true,
            ipAddress = "10.0.0.5/24",
            rxBytesPerSec = 520_000L,
            txBytesPerSec = 85_000L,
            totalRxBytes = 124_000_000_000L,
            totalTxBytes = 42_000_000_000L,
            rxHistory = listOf(0.4f, 0.5f),
            txHistory = listOf(0.08f, 0.09f),
        )

        val testNetworkData = NetworkIoData(
            interfaces = listOf(eth0Interface, eth1Interface),
            selectedInterfaceName = "eth0",
        )

        val initialViewState = ViewState()

        val readyViewState = ViewState(
            overallStatus = StatusLevel.Operational,
            interfaces = listOf(eth0Interface, eth1Interface),
            selectedInterface = eth0Interface,
            aggregateRxBytesPerSec = 14_850_000L + 520_000L,
            aggregateTxBytesPerSec = 3_210_000L + 85_000L,
            activeInterfacesCount = 2,
            widgetState = WidgetState.Ready,
        )
    }
}
