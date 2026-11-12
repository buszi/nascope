package io.buszi.nascope.domain.model.telemetry

data class NetworkInterfaceUsage(
    val name: String,
    val alias: String?,
    /** Absent in Glances 4.5.9 payloads. */
    val isUp: Boolean?,
    /** Glances reports 0 for unknown, which maps to null. */
    val speedBitsPerSec: Long?,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
)
