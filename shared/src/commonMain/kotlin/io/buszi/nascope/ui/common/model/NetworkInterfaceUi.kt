package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.ui.common.components.badges.StatusLevel
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
    val macAddress: String = "",
    val mtu: Int = 0,
    val linkSpeedMbps: Int = 0,
    val rxErrors: Long = 0L,
    val txErrors: Long = 0L,
) {
    /** Peak of [rxHistory] in bytes per second; history samples are in MB/s. */
    val peakRxBytesPerSec: Long
        get() = (rxHistory.maxOrNull() ?: 0f).toBytesPerSec()

    /** Peak of [txHistory] in bytes per second; history samples are in MB/s. */
    val peakTxBytesPerSec: Long
        get() = (txHistory.maxOrNull() ?: 0f).toBytesPerSec()

    /** Share of the link capacity used by the busier direction in `[0.0, 1.0]`, or `null` when the speed is unknown. */
    val linkUtilization: Float?
        get() = linkSpeedMbps
            .takeIf { it > 0 }
            ?.let { (maxOf(rxBytesPerSec, txBytesPerSec) * BitsPerByte / (it * BitsPerMegabit)).coerceIn(0f, 1f) }

    val totalErrors: Long
        get() = rxErrors + txErrors

    private fun Float.toBytesPerSec(): Long = (this * BytesPerMegabyte).toLong()

    companion object {
        private const val BitsPerByte: Float = 8f
        private const val BitsPerMegabit: Float = 1_000_000f
        private const val BytesPerMegabyte: Float = 1_000_000f

        /** Network is online while at least one interface is up and offline (critical) otherwise. */
        fun overallStatus(interfaces: List<NetworkInterfaceUi>): StatusLevel =
            if (interfaces.any { it.isUp }) StatusLevel.Operational else StatusLevel.Critical

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
            macAddress = data.macAddress,
            mtu = data.mtu,
            linkSpeedMbps = data.linkSpeedMbps,
            rxErrors = data.rxErrors,
            txErrors = data.txErrors,
        )
    }
}
