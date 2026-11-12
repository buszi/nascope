package io.buszi.nascope.domain.model.telemetry

data class FileSystemUsage(
    val mountPoint: String,
    val deviceName: String?,
    val fsType: String?,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    /** Glances 4 only. */
    val options: String?,
)
