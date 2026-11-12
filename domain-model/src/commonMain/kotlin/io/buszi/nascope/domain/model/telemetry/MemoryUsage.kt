package io.buszi.nascope.domain.model.telemetry

data class MemoryUsage(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val availableBytes: Long?,
    val cachedBytes: Long?,
    val buffersBytes: Long?,
)
