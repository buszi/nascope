package io.buszi.nascope.domain.model.telemetry

data class SwapUsage(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
)
