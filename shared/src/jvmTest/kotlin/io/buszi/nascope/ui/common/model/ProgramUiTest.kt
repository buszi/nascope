package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgramUiTest {

    private val usage = ProgramUsage("php-fpm", 12, 200f, 4f, 600L, 24, null, "S")

    @Test
    fun mapsTheFiguresAndAMissingUserToADash() {
        val ui = ProgramUi.from(usage, logicalCores = 4)
        assertEquals("php-fpm", ui.name)
        assertEquals(12, ui.processCount)
        assertEquals(600L, ui.memoryRssBytes)
        assertEquals(24, ui.threads)
        assertEquals(ProcessUi.MissingUser, ui.user)
    }

    @Test
    fun cpuIsJudgedPerMachineLikeAProcess() {
        // 200% over four cores is half the machine: degraded, not critical.
        val ui = ProgramUi.from(usage, logicalCores = 4)
        assertEquals(50f, ui.cpuMachinePercent)
        assertEquals(StatusLevel.Degraded, ui.loadStatus)
        // Without the core count the host counts as one core and the share is capped at 100.
        val single = ProgramUi.from(usage, logicalCores = null)
        assertEquals(100f, single.cpuMachinePercent)
        assertEquals(StatusLevel.Critical, single.loadStatus)
    }

    @Test
    fun memoryAloneCanMakeAProgramLoaded() {
        assertEquals(StatusLevel.Critical, ProgramUi.from(usage.copy(cpuPercent = 0f, memoryPercent = 85f), 4).loadStatus)
        assertEquals(StatusLevel.Operational, ProgramUi.from(usage.copy(cpuPercent = 4f, memoryPercent = 3f), 4).loadStatus)
    }
}
