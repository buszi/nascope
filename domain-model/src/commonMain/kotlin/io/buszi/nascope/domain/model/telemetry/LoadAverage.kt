package io.buszi.nascope.domain.model.telemetry

data class LoadAverage(
    val min1: Float,
    val min5: Float,
    val min15: Float,
    val logicalCores: Int?,
)
