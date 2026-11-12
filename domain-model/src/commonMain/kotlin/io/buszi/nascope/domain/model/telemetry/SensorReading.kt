package io.buszi.nascope.domain.model.telemetry

data class SensorReading(
    val label: String,
    val kind: SensorKind,
    val value: Float,
    /** "C", "F", "R" (rpm) or "%". */
    val unit: String?,
    val warning: Float?,
    val critical: Float?,
)
