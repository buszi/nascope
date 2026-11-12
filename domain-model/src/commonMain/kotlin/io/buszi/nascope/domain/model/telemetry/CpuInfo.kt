package io.buszi.nascope.domain.model.telemetry

data class CpuInfo(
    val name: String?,
    val currentFrequencyHz: Long?,
    val maxFrequencyHz: Long?,
)
