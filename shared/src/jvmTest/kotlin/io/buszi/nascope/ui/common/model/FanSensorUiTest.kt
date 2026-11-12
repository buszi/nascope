package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Sensor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FanSensorUiTest {

    @Test
    fun fanWithoutRotationIsStalled() {
        assertTrue(FanSensorUi(label = "CPU Fan", rpm = 0).isStalled)
        assertFalse(FanSensorUi(label = "CPU Fan", rpm = 900).isStalled)
    }

    @Test
    fun fromMapsTheReadingToRpmWithItsHistory() {
        val ui = FanSensorUi.from(
            Sensor(SensorReading("fan1", SensorKind.FanSpeed, 1199.6f, "R", null, null), listOf(1000f, 1200f)),
        )

        assertEquals(FanSensorUi("fan1", 1200, listOf(1000f, 1200f)), ui)
    }
}
