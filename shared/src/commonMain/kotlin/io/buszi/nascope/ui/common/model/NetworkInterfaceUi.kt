package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a network interface and its throughput. Rates and history samples are in bytes per second.
 */
@Immutable
data class NetworkInterfaceUi(
    val name: String,
    val isUp: Boolean,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val rxHistory: List<Float> = emptyList(),
    val txHistory: List<Float> = emptyList(),
    /** `null` when Glances does not know the link speed: link utilization is not shown. */
    val linkSpeedMbps: Int? = null,
) {
    /** Peak of [rxHistory] in bytes per second. */
    val peakRxBytesPerSec: Long
        get() = (rxHistory.maxOrNull() ?: 0f).toLong()

    /** Peak of [txHistory] in bytes per second. */
    val peakTxBytesPerSec: Long
        get() = (txHistory.maxOrNull() ?: 0f).toLong()

    /** Share of the link capacity used by the busier direction in `[0.0, 1.0]`, or `null` when the speed is unknown. */
    val linkUtilization: Float?
        get() = linkSpeedMbps
            ?.takeIf { it > 0 }
            ?.let { (maxOf(rxBytesPerSec, txBytesPerSec) * BitsPerByte / (it * BitsPerMegabit)).coerceIn(0f, 1f) }

    companion object {
        private const val BitsPerByte: Float = 8f
        private const val BitsPerMegabit: Float = 1_000_000f
        private const val LoopbackName = "lo"

        /** Network is online while at least one interface is up and offline (critical) otherwise. */
        fun overallStatus(interfaces: List<NetworkInterfaceUi>): StatusLevel =
            if (interfaces.any { it.isUp }) StatusLevel.Operational else StatusLevel.Critical

        /** `isUp == null` (Glances 4) counts as up. */
        fun from(data: ObserveNetworkUseCase.Interface) = NetworkInterfaceUi(
            name = data.usage.name,
            isUp = data.usage.isUp ?: true,
            rxBytesPerSec = data.usage.rxBytesPerSec,
            txBytesPerSec = data.usage.txBytesPerSec,
            totalRxBytes = data.usage.totalRxBytes,
            totalTxBytes = data.usage.totalTxBytes,
            rxHistory = data.rxHistory,
            txHistory = data.txHistory,
            linkSpeedMbps = data.usage.speedBitsPerSec?.let { (it / 1_000_000L).toInt() },
        )

        /** The interfaces to show: the loopback interface is left out. */
        fun fromAll(data: ObserveNetworkUseCase.Data): List<NetworkInterfaceUi> =
            data.interfaces.filter { it.usage.name != LoopbackName }.map(::from)
    }
}
