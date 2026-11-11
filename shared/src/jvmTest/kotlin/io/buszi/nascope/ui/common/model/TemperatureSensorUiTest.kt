package io.buszi.nascope.ui.common.model

import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class TemperatureSensorUiTest {

    @Test
    fun statusFollowsThresholds() {
        assertEquals(StatusLevel.Operational, sensor(69f).status)
        assertEquals(StatusLevel.Degraded, sensor(70f).status)
        assertEquals(StatusLevel.Critical, sensor(85f).status)
    }

    @Test
    fun progressIsNormalizedWithinRange() {
        val range = 30f..100f
        assertEquals(0.5f, sensor(65f).progressIn(range))
        assertEquals(0f, sensor(10f).progressIn(range))
        assertEquals(1f, sensor(120f).progressIn(range))
        assertEquals(40f / 70f, sensor(65f).warningProgressIn(range))
        assertEquals(55f / 70f, sensor(65f).criticalProgressIn(range))
    }

    @Test
    fun historyStatsAreDerivedFromHistory() {
        val sensor = sensor(60f, history = listOf(50f, 60f, 70f))

        assertEquals(50f, sensor.minHistoryC)
        assertEquals(70f, sensor.maxHistoryC)
        assertEquals(60f, sensor.averageHistoryC)
    }

    @Test
    fun historyStatsFallBackToCurrentTemperatureWithoutHistory() {
        val sensor = sensor(61f)

        assertEquals(61f, sensor.minHistoryC)
        assertEquals(61f, sensor.maxHistoryC)
        assertEquals(61f, sensor.averageHistoryC)
    }

    @Test
    fun headroomIsDistanceToThresholds() {
        val sensor = sensor(60f)

        assertEquals(10f, sensor.headroomToWarningC)
        assertEquals(25f, sensor.headroomToCriticalC)
        assertEquals(-5f, sensor(75f).headroomToWarningC)
    }

    private fun sensor(currentTempC: Float, history: List<Float> = emptyList()) = TemperatureSensorUi(
        label = "CPU",
        currentTempC = currentTempC,
        warningThresholdC = 70f,
        criticalThresholdC = 85f,
        history = history,
    )
}
