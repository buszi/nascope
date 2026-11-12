package io.buszi.nascope.domain.model.telemetry

data class ContainerUsage(
    val id: String,
    val name: String,
    val image: String?,
    /** "docker" or "podman". */
    val engine: String?,
    val status: ContainerStatus,
    val cpuPercent: Float?,
    val memoryUsageBytes: Long?,
    val memoryLimitBytes: Long?,
    /** Passed through as Glances formats it ("10 secs"); the only pre-formatted value. */
    val uptimeText: String?,
)
