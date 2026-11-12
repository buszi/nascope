package io.buszi.nascope.domain.model.telemetry

/** One GPU as Glances reports it. Integrated GPUs usually report the utilization only. */
data class GpuUsage(
    val id: String,
    val name: String,
    /** Processor utilization in percent; `null` when the driver does not report it. */
    val utilizationPercent: Float?,
    /** Memory use in percent; `null` for integrated GPUs, which share the system memory. */
    val memoryPercent: Float?,
    /** Degrees Celsius; `null` when not reported. */
    val temperatureC: Float?,
    /** Fan speed in percent; `null` when not reported. */
    val fanSpeedPercent: Float?,
)
