package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Sensor
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SensorsUiTest {

    private fun temp(label: String, value: Float, unit: String? = "C", kind: SensorKind = SensorKind.CpuTemperature) =
        Sensor(SensorReading(label, kind, value, unit, null, null), emptyList())

    @Test
    fun mapsTheNasShapedGroupsAndHasNoFans() {
        val ui = SensorsUi.from(
            Data(
                cpu = listOf(temp("Package id 0", 55f), temp("Core 0", 52f)),
                drives = listOf(temp("Composite", 40f), temp("Sensor 2", 38f)),
                fans = emptyList(),
                other = listOf(temp("acpitz 0", 27f)),
            ),
        )

        assertEquals(listOf("Package id 0", "Core 0"), ui.cpu.map { it.label })
        assertEquals(listOf("Composite", "Sensor 2"), ui.drives.map { it.label })
        assertEquals(listOf("acpitz 0"), ui.other.map { it.label })
        assertTrue(ui.fans.isEmpty())
        assertEquals(55f, ui.cpuHeadline?.currentTempC)
        assertEquals("Package id 0", ui.hottest?.label)
    }

    @Test
    fun theCpuHeadlineFallsBackToTheHottestCoreAndThenToNull() {
        val coresOnly = SensorsUi(cpu = listOf(TemperatureSensorUi("Core 0", 50f, 70f, 85f), TemperatureSensorUi("Core 1", 62f, 70f, 85f)))

        assertEquals("Core 1", coresOnly.cpuHeadline?.label)
        assertNull(SensorsUi().cpuHeadline)
    }

    @Test
    fun readingsInOtherUnitsAreNotShownAsTemperatures() {
        val ui = SensorsUi.from(
            Data(
                cpu = emptyList(),
                drives = emptyList(),
                fans = listOf(Sensor(SensorReading("fan1", SensorKind.FanSpeed, 1200f, "R", null, null), emptyList())),
                other = listOf(temp("Battery", 80f, unit = "%", kind = SensorKind.Battery), temp("acpitz 0", 27f), temp("x", 90f, unit = null)),
            ),
        )

        assertEquals(listOf("acpitz 0"), ui.other.map { it.label })
        assertEquals(1, ui.fans.size)
    }

    @Test
    fun statusRiskAndFanCountsAreDerivedFromTheGroups() {
        val ui = SensorsUi(
            cpu = listOf(TemperatureSensorUi("Package id 0", 75f, 70f, 85f)),
            drives = listOf(TemperatureSensorUi("Composite", 90f, 70f, 85f)),
            fans = listOf(FanSensorUi("a", 1000), FanSensorUi("b", 0)),
        )

        assertEquals(StatusLevel.Critical, ui.overallStatus)
        assertEquals(2, ui.atRiskCount)
        assertEquals(1, ui.activeFansCount)
        assertEquals(false, ui.isEmpty)
        assertTrue(SensorsUi().isEmpty)
        assertEquals(StatusLevel.Operational, SensorsUi().overallStatus)
    }
}
