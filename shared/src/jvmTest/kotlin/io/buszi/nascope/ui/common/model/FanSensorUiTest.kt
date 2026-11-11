package io.buszi.nascope.ui.common.model

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
    fun dutyCycleDefaultsToRpmOverMaxRpm() {
        assertEquals(0.5f, FanSensorUi(label = "CPU Fan", rpm = 1000, maxRpm = 2000).dutyCyclePercent)
    }
}
