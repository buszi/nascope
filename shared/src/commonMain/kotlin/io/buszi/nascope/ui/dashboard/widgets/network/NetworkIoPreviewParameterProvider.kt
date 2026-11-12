package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewState

class NetworkIoPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Nominal / Healthy State
        ViewState(
            overallStatus = StatusLevel.Operational,
            interfaces = listOf(
                NetworkInterfaceUi(
                    name = "eth0",
                    isUp = true,
                    rxBytesPerSec = 14_850_000L,
                    txBytesPerSec = 3_210_000L,
                    totalRxBytes = 842_300_000_000L,
                    totalTxBytes = 195_600_000_000L,
                    rxHistory = listOf(8200000f, 10500000f, 12100000f, 15400000f, 14800000f, 18200000f, 21000000f, 19500000f, 16300000f, 14850000f),
                    txHistory = listOf(1800000f, 2100000f, 2500000f, 3800000f, 3200000f, 4100000f, 4500000f, 3900000f, 3000000f, 3210000f),
                ),
                NetworkInterfaceUi(
                    name = "eth1",
                    isUp = true,
                    rxBytesPerSec = 520_000L,
                    txBytesPerSec = 85_000L,
                    totalRxBytes = 124_000_000_000L,
                    totalTxBytes = 42_000_000_000L,
                    rxHistory = listOf(300000f, 400000f, 500000f, 600000f, 520000f, 480000f, 550000f, 510000f, 520000f, 520000f),
                    txHistory = listOf(50000f, 80000f, 90000f, 85000f, 70000f, 80000f, 90000f, 85000f, 80000f, 85000f),
                ),
                NetworkInterfaceUi(
                    name = "wlan0",
                    isUp = false,
                    rxBytesPerSec = 0L,
                    txBytesPerSec = 0L,
                    totalRxBytes = 1_200_000_000L,
                    totalTxBytes = 450_000_000L,
                    rxHistory = List(10) { 0f },
                    txHistory = List(10) { 0f },
                ),
            ),
            selectedInterface = NetworkInterfaceUi(
                name = "eth0",
                isUp = true,
                rxBytesPerSec = 14_850_000L,
                txBytesPerSec = 3_210_000L,
                totalRxBytes = 842_300_000_000L,
                totalTxBytes = 195_600_000_000L,
                rxHistory = listOf(8200000f, 10500000f, 12100000f, 15400000f, 14800000f, 18200000f, 21000000f, 19500000f, 16300000f, 14850000f),
                txHistory = listOf(1800000f, 2100000f, 2500000f, 3800000f, 3200000f, 4100000f, 4500000f, 3900000f, 3000000f, 3210000f),
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
                NetworkInterfaceUi(
                    name = "eth0",
                    isUp = false,
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

        // 3. Stale
        ViewState(
            overallStatus = StatusLevel.Operational,
            interfaces = listOf(eth0),
            selectedInterface = eth0,
            aggregateRxBytesPerSec = 14_850_000L,
            aggregateTxBytesPerSec = 3_210_000L,
            activeInterfacesCount = 1,
            widgetState = WidgetState.Stale,
        ),

        // 4. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 5. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )

    private companion object {
        val eth0 = NetworkInterfaceUi(
            name = "eth0",
            isUp = true,
            rxBytesPerSec = 14_850_000L,
            txBytesPerSec = 3_210_000L,
            totalRxBytes = 842_300_000_000L,
            totalTxBytes = 195_600_000_000L,
            rxHistory = listOf(8_200_000f, 10_500_000f, 12_100_000f, 15_400_000f, 14_800_000f),
            txHistory = listOf(1_800_000f, 2_100_000f, 2_500_000f, 3_800_000f, 3_200_000f),
        )
    }
}
