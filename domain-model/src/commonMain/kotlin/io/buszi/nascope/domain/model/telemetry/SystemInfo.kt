package io.buszi.nascope.domain.model.telemetry

data class SystemInfo(
    val hostname: String,
    val osName: String,
    /** Kernel release on Linux. */
    val osVersion: String?,
    val distribution: String?,
    val architecture: String?,
    val displayName: String?,
)
