package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase.Data
import io.buszi.nascope.ui.dashboard.widgets.system.HostPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SystemOverviewUiTest {

    @Test
    fun formatsUptimeDroppingZeroDaysAndHours() {
        assertEquals("14d 06h 32m", SystemOverviewUi.formatUptime(14.days + 6.hours + 32.minutes))
        assertEquals("3h 05m", SystemOverviewUi.formatUptime(3.hours + 5.minutes))
        assertEquals("0m", SystemOverviewUi.formatUptime(20.seconds))
    }

    @Test
    fun usesTheDistributionAndFallsBackToTheOsNameWhenPartsAreMissing() {
        val ui = SystemOverviewUi.from(
            Data(SystemInfo("mac", "Darwin", null, null, null, null), uptime = null, load = null),
        )

        assertEquals("Darwin", ui.osName)
        assertEquals("Darwin", ui.kernelRelease)
        assertEquals(HostPlatform.MacOS, ui.platform)
        assertEquals(null, ui.uptime)
        assertEquals(null, ui.load)
    }
}
