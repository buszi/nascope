package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SensorClassifierTest {

    private fun core(label: String, value: Float = 40f, kind: SensorKind = SensorKind.CpuTemperature) =
        SensorReading(label, kind, value, "C", null, null)

    @Test
    fun classifiesTheNasLabelsOfBothGlancesVersions() {
        // Same eight labels in v4-nas and v3-nas (only the order differs); all arrive as temperature_core.
        val expected = mapOf(
            "Composite" to SensorGroup.Drives,
            "Core 0" to SensorGroup.Cpu,
            "Core 1" to SensorGroup.Cpu,
            "Core 2" to SensorGroup.Cpu,
            "Core 3" to SensorGroup.Cpu,
            "Package id 0" to SensorGroup.Cpu,
            "Sensor 2" to SensorGroup.Drives,
            "acpitz 0" to SensorGroup.Other,
        )

        assertEquals(expected, expected.keys.associateWith { SensorClassifier.classify(core(it)) })
    }

    @Test
    fun classifiesTheSyntheticDriveFanAndBatteryTypes() {
        assertEquals(SensorGroup.Drives, SensorClassifier.classify(core("sda", kind = SensorKind.DriveTemperature)))
        assertEquals(SensorGroup.Fans, SensorClassifier.classify(core("fan1", kind = SensorKind.FanSpeed)))
        assertEquals(SensorGroup.Other, SensorClassifier.classify(core("Battery", kind = SensorKind.Battery)))
    }

    @Test
    fun theKindWinsOverTheLabel() {
        assertEquals(SensorGroup.Fans, SensorClassifier.classify(core("Core 0", kind = SensorKind.FanSpeed)))
    }

    @Test
    fun matchesLabelsCaseInsensitivelyAndRecognizesNvmeLabels() {
        assertEquals(SensorGroup.Cpu, SensorClassifier.classify(core("PACKAGE ID 1")))
        assertEquals(SensorGroup.Drives, SensorClassifier.classify(core("NVMe0n1 Composite")))
        assertEquals(SensorGroup.Drives, SensorClassifier.classify(core("composite")))
        assertEquals(SensorGroup.Other, SensorClassifier.classify(core("Corey 1")))
    }

    @Test
    fun cpuTemperatureIsTheHighestPackageReading() {
        val readings = listOf(core("Core 0", 80f), core("Package id 0", 55f), core("Package id 1", 60f))

        assertEquals(60f, SensorClassifier.cpuTemperature(readings))
    }

    @Test
    fun cpuTemperatureFallsBackToTheHighestCore() {
        val readings = listOf(core("Core 0", 50f), core("Core 1", 62f), core("Composite", 90f))

        assertEquals(62f, SensorClassifier.cpuTemperature(readings))
    }

    @Test
    fun cpuTemperatureIsNullWithoutPackageOrCoreReadings() {
        assertNull(SensorClassifier.cpuTemperature(listOf(core("acpitz 0", 30f), core("Composite", 40f))))
        assertNull(SensorClassifier.cpuTemperature(emptyList()))
    }
}
