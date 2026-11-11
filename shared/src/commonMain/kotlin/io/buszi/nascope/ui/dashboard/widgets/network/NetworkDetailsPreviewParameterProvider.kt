package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewState

class NetworkDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Two active interfaces and a disconnected one (collapsed)
        ViewState(
            interfaces = previewInterfaces,
            collapsedNames = setOf("wlan0"),
            widgetState = WidgetState.Ready,
        ),

        // 2. Everything down
        ViewState(
            interfaces = previewInterfaces.map { it.copy(isUp = false, rxBytesPerSec = 0L, txBytesPerSec = 0L) },
            widgetState = WidgetState.Ready,
        ),

        // 3. No interfaces
        ViewState(widgetState = WidgetState.Ready),

        // 4. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        val previewInterfaces = listOf(
            NetworkInterfaceUi(
                name = "eth0",
                isUp = true,
                ipAddress = "192.168.1.120/24",
                rxBytesPerSec = 14_850_000L,
                txBytesPerSec = 3_210_000L,
                totalRxBytes = 842_300_000_000L,
                totalTxBytes = 195_600_000_000L,
                rxHistory = listOf(8.2f, 10.5f, 12.1f, 15.4f, 14.8f, 18.2f, 21.0f, 19.5f, 16.3f, 14.0f, 13.8f, 15.2f),
                txHistory = listOf(1.8f, 2.1f, 2.5f, 3.8f, 3.2f, 4.1f, 4.5f, 3.9f, 3.0f, 2.8f, 3.1f, 3.5f),
                macAddress = "a8:5e:45:3c:91:d2",
                mtu = 1500,
                linkSpeedMbps = 1000,
            ),
            NetworkInterfaceUi(
                name = "eth1",
                isUp = true,
                ipAddress = "10.0.0.5/24",
                rxBytesPerSec = 520_000L,
                txBytesPerSec = 85_000L,
                totalRxBytes = 124_000_000_000L,
                totalTxBytes = 42_000_000_000L,
                rxHistory = listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.52f, 0.48f, 0.55f, 0.51f),
                txHistory = listOf(0.05f, 0.08f, 0.09f, 0.085f, 0.07f, 0.08f, 0.09f, 0.085f),
                macAddress = "a8:5e:45:3c:91:d3",
                mtu = 9000,
                linkSpeedMbps = 100,
                rxErrors = 12L,
                txErrors = 3L,
            ),
            NetworkInterfaceUi(
                name = "wlan0",
                isUp = false,
                ipAddress = "Disconnected",
                rxBytesPerSec = 0L,
                txBytesPerSec = 0L,
                totalRxBytes = 1_200_000_000L,
                totalTxBytes = 450_000_000L,
                rxHistory = List(8) { 0f },
                txHistory = List(8) { 0f },
                macAddress = "f4:6d:3f:0a:be:17",
                mtu = 1500,
            ),
        )
    }
}
