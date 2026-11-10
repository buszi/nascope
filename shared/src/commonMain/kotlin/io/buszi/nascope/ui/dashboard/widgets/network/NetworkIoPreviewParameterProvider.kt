package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.data.telemetry.NetworkInterfaceData
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewState

class NetworkIoPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Nominal / Healthy State
        ViewState(
            overallStatus = StatusLevel.Operational,
            interfaces = listOf(
                NetworkInterfaceData(
                    name = "eth0",
                    isUp = true,
                    ipAddress = "192.168.1.120/24",
                    rxBytesPerSec = 14_850_000L,
                    txBytesPerSec = 3_210_000L,
                    totalRxBytes = 842_300_000_000L,
                    totalTxBytes = 195_600_000_000L,
                    rxHistory = listOf(8.2f, 10.5f, 12.1f, 15.4f, 14.8f, 18.2f, 21.0f, 19.5f, 16.3f, 14.85f),
                    txHistory = listOf(1.8f, 2.1f, 2.5f, 3.8f, 3.2f, 4.1f, 4.5f, 3.9f, 3.0f, 3.21f),
                ),
                NetworkInterfaceData(
                    name = "eth1",
                    isUp = true,
                    ipAddress = "10.0.0.5/24",
                    rxBytesPerSec = 520_000L,
                    txBytesPerSec = 85_000L,
                    totalRxBytes = 124_000_000_000L,
                    totalTxBytes = 42_000_000_000L,
                    rxHistory = listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.52f, 0.48f, 0.55f, 0.51f, 0.52f, 0.52f),
                    txHistory = listOf(0.05f, 0.08f, 0.09f, 0.085f, 0.07f, 0.08f, 0.09f, 0.085f, 0.08f, 0.085f),
                ),
                NetworkInterfaceData(
                    name = "wlan0",
                    isUp = false,
                    ipAddress = "Disconnected",
                    rxBytesPerSec = 0L,
                    txBytesPerSec = 0L,
                    totalRxBytes = 1_200_000_000L,
                    totalTxBytes = 450_000_000L,
                    rxHistory = List(10) { 0f },
                    txHistory = List(10) { 0f },
                ),
            ),
            selectedInterface = NetworkInterfaceData(
                name = "eth0",
                isUp = true,
                ipAddress = "192.168.1.120/24",
                rxBytesPerSec = 14_850_000L,
                txBytesPerSec = 3_210_000L,
                totalRxBytes = 842_300_000_000L,
                totalTxBytes = 195_600_000_000L,
                rxHistory = listOf(8.2f, 10.5f, 12.1f, 15.4f, 14.8f, 18.2f, 21.0f, 19.5f, 16.3f, 14.85f),
                txHistory = listOf(1.8f, 2.1f, 2.5f, 3.8f, 3.2f, 4.1f, 4.5f, 3.9f, 3.0f, 3.21f),
            ),
            aggregateRxBytesPerSec = 15_370_000L,
            aggregateTxBytesPerSec = 3_295_000L,
            activeInterfacesCount = 2,
            widgetState = WidgetState.Ready,
        ),

        // 2. Degraded / Critical State (All down)
        ViewState(
            overallStatus = StatusLevel.Critical,
            interfaces = listOf(
                NetworkInterfaceData(
                    name = "eth0",
                    isUp = false,
                    ipAddress = "Link Down",
                    rxBytesPerSec = 0L,
                    txBytesPerSec = 0L,
                    totalRxBytes = 10_000_000L,
                    totalTxBytes = 5_000_000L,
                    rxHistory = List(10) { 0f },
                    txHistory = List(10) { 0f },
                ),
            ),
            selectedInterface = null,
            aggregateRxBytesPerSec = 0L,
            aggregateTxBytesPerSec = 0L,
            activeInterfacesCount = 0,
            widgetState = WidgetState.Ready,
        ),

        // 3. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )
}
