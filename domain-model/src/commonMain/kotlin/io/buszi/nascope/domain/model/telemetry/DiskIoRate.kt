package io.buszi.nascope.domain.model.telemetry

data class DiskIoRate(
    val diskName: String,
    val readBytesPerSec: Long,
    val writeBytesPerSec: Long,
)
