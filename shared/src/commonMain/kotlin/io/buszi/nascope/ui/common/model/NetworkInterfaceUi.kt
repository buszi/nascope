package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.NetworkInterfaceData

/**
 * UI representation of a network interface and its throughput.
 */
@Immutable
data class NetworkInterfaceUi(
    val name: String,
    val isUp: Boolean,
    val ipAddress: String,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val rxHistory: List<Float> = emptyList(),
    val txHistory: List<Float> = emptyList(),
) {
    companion object {
        fun from(data: NetworkInterfaceData) = NetworkInterfaceUi(
            name = data.name,
            isUp = data.isUp,
            ipAddress = data.ipAddress,
            rxBytesPerSec = data.rxBytesPerSec,
            txBytesPerSec = data.txBytesPerSec,
            totalRxBytes = data.totalRxBytes,
            totalTxBytes = data.totalTxBytes,
            rxHistory = data.rxHistory,
            txHistory = data.txHistory,
        )
    }
}
