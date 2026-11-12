package io.buszi.nascope.domain.model.telemetry

data class ProcessCount(
    val total: Int,
    val running: Int,
    val sleeping: Int,
    val threads: Int,
)
