package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading

internal enum class SensorGroup { Cpu, Drives, Fans, Other }

/**
 * Glances reports every NAS temperature as `temperature_core`, so the label decides (case-insensitively):
 * fan / battery / drive kinds first, then CPU labels, then drive-like labels, everything else is "other".
 */
internal object SensorClassifier {

    private val PackageLabel = Regex("package id \\d+")
    private val CoreLabel = Regex("core \\d+")
    private val DriveSensorLabel = Regex("sensor \\d+")

    fun classify(reading: SensorReading): SensorGroup {
        when (reading.kind) {
            SensorKind.FanSpeed -> return SensorGroup.Fans
            SensorKind.Battery -> return SensorGroup.Other
            SensorKind.DriveTemperature -> return SensorGroup.Drives
            SensorKind.CpuTemperature, SensorKind.Other -> Unit
        }
        val label = reading.label.trim().lowercase()
        return when {
            PackageLabel.matches(label) || CoreLabel.matches(label) -> SensorGroup.Cpu
            label == "composite" || DriveSensorLabel.matches(label) || label.startsWith("nvme") -> SensorGroup.Drives
            else -> SensorGroup.Other
        }
    }

    /** The highest `Package id N` reading, else the highest `Core N` reading, else `null`. Raw unit. */
    fun cpuTemperature(readings: List<SensorReading>): Float? {
        val cpu = readings.filter { classify(it) == SensorGroup.Cpu }
        fun highest(pattern: Regex) = cpu.filter { pattern.matches(it.label.trim().lowercase()) }.maxOfOrNull { it.value }
        return highest(PackageLabel) ?: highest(CoreLabel)
    }
}
