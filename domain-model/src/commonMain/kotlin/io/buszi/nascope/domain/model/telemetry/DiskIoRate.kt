package io.buszi.nascope.domain.model.telemetry

/**
 * The I/O of one disk as Glances reports it.
 *
 * The operation rates and latencies are `null` when the host cannot tell: API v4 reports both, API v3 only the
 * operation counts (so [readIops] and [writeIops] are derived there) and never the latency.
 */
data class DiskIoRate(
    val diskName: String,
    val readBytesPerSec: Long,
    val writeBytesPerSec: Long,
    val readIops: Long? = null,
    val writeIops: Long? = null,
    /** Average time of a read, in milliseconds. */
    val readLatencyMs: Long? = null,
    val writeLatencyMs: Long? = null,
)
