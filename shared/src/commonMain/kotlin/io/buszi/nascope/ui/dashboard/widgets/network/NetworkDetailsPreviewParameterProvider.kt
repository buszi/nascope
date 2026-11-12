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

        // 4. Glances 4 payload: link speed unknown, no utilization shown
        ViewState(
            interfaces = previewInterfaces.take(1).map { it.copy(linkSpeedMbps = null) },
            widgetState = WidgetState.Stale,
        ),

        // 5. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 6. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        val previewInterfaces = listOf(
            NetworkInterfaceUi(
                name = "eth0",
                isUp = true,
                rxBytesPerSec = 14_850_000L,
                txBytesPerSec = 3_210_000L,
                totalRxBytes = 842_300_000_000L,
                totalTxBytes = 195_600_000_000L,
                rxHistory = listOf(8200000f, 10500000f, 12100000f, 15400000f, 14800000f, 18200000f, 21000000f, 19500000f, 16300000f, 14000000f, 13800000f, 15200000f),
                txHistory = listOf(1800000f, 2100000f, 2500000f, 3800000f, 3200000f, 4100000f, 4500000f, 3900000f, 3000000f, 2800000f, 3100000f, 3500000f),
                linkSpeedMbps = 1000,
            ),
            NetworkInterfaceUi(
                name = "eth1",
                isUp = true,
                rxBytesPerSec = 520_000L,
                txBytesPerSec = 85_000L,
                totalRxBytes = 124_000_000_000L,
                totalTxBytes = 42_000_000_000L,
                rxHistory = listOf(300000f, 400000f, 500000f, 600000f, 520000f, 480000f, 550000f, 510000f),
                txHistory = listOf(50000f, 80000f, 90000f, 85000f, 70000f, 80000f, 90000f, 85000f),
                linkSpeedMbps = 100,
            ),
            NetworkInterfaceUi(
                name = "wlan0",
                isUp = false,
                rxBytesPerSec = 0L,
                txBytesPerSec = 0L,
                totalRxBytes = 1_200_000_000L,
                totalTxBytes = 450_000_000L,
                rxHistory = List(8) { 0f },
                txHistory = List(8) { 0f },
            ),
        )
    }
}
