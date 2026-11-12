package io.buszi.nascope.domain.model.telemetry

data class ProcessUsage(
    val pid: Int,
    val name: String,
    val user: String?,
    val cpuPercent: Float,
    val memoryPercent: Float,
    val memoryRssBytes: Long?,
    /** psutil status letter: R, S, I, Z, ... */
    val statusCode: String?,
    val commandLine: List<String>?,
)
