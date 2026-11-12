package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class CpuCoreUiTest {

    @Test
    fun fromMapsDomainCore() {
        assertEquals(CpuCoreUi(index = 3, usagePercent = 40f), CpuCoreUi.from(CpuCoreUsage(3, 40f)))
    }

    @Test
    fun usageStatusFollowsThresholds() {
        assertEquals(StatusLevel.Operational, CpuCoreUi.usageStatus(74.9f))
        assertEquals(StatusLevel.Degraded, CpuCoreUi.usageStatus(75f))
        assertEquals(StatusLevel.Critical, CpuCoreUi.usageStatus(90f))
    }

    @Test
    fun temperatureStatusFollowsThresholds() {
        assertEquals(StatusLevel.Operational, CpuCoreUi.temperatureStatus(74f))
        assertEquals(StatusLevel.Degraded, CpuCoreUi.temperatureStatus(75f))
        assertEquals(StatusLevel.Critical, CpuCoreUi.temperatureStatus(85f))
    }

    @Test
    fun overallStatusIsWorstOfUsageAndTemperature() {
        assertEquals(StatusLevel.Operational, CpuCoreUi.overallStatus(40f, 50f))
        assertEquals(StatusLevel.Degraded, CpuCoreUi.overallStatus(80f, 50f))
        assertEquals(StatusLevel.Degraded, CpuCoreUi.overallStatus(40f, 76f))
        assertEquals(StatusLevel.Critical, CpuCoreUi.overallStatus(80f, 90f))
    }

    @Test
    fun usageRatioIsClampedToUnitRange() {
        assertEquals(0.5f, CpuCoreUi(0, 50f).usageRatio)
        assertEquals(1f, CpuCoreUi(0, 140f).usageRatio)
    }
}
