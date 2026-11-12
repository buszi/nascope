package io.buszi.nascope.domain.model.telemetry

/** The processes of one program (`programlist` plugin), summed by Glances. Only served by Glances 4. */
data class ProgramUsage(
    val name: String,
    /** How many processes the program runs. */
    val processCount: Int,
    /** Sum of the processes' CPU, per core like a process (so it can exceed 100). */
    val cpuPercent: Float,
    val memoryPercent: Float,
    val memoryRssBytes: Long?,
    val threads: Int?,
    val user: String?,
    /** psutil status letter of the program as Glances reports it. */
    val statusCode: String?,
)
