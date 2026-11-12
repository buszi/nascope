package io.buszi.nascope.domain.model.telemetry

data class CpuUsage(
    val totalPercent: Float,
    val userPercent: Float?,
    val systemPercent: Float?,
    val ioWaitPercent: Float?,
    val idlePercent: Float?,
    val logicalCores: Int?,
)
