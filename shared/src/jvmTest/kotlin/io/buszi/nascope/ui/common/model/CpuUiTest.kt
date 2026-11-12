package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase.Data
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CpuUiTest {

    private val usage = CpuUsage(42.8f, null, null, null, null, 8)

    @Test
    fun mapsAFullyPopulatedUseCaseResult() {
        val ui = CpuUi.from(
            Data(
                usage = usage,
                cores = listOf(CpuCoreUsage(0, 10f), CpuCoreUsage(1, 90f)),
                info = CpuInfo("Ryzen 9", 3_800_000_000, 4_900_000_000),
                temperature = 58f,
                usageHistory = listOf(40f, 45f),
                usageDelta = 2.44f,
            ),
        )

        assertEquals(
            CpuUi(
                modelName = "Ryzen 9",
                usagePercent = 42.8f,
                trend = "+2.4%",
                frequencyGhz = 3.8f,
                maxFrequencyGhz = 4.9f,
                temperatureC = 58f,
                cores = listOf(CpuCoreUi(0, 10f), CpuCoreUi(1, 90f)),
                usageHistory = listOf(40f, 45f),
            ),
            ui,
        )
    }

    @Test
    fun leavesOutWhatTheHostDoesNotProvide() {
        val ui = CpuUi.from(Data(usage, emptyList(), null, null, emptyList(), null))

        assertEquals("", ui.modelName)
        assertNull(ui.trend)
        assertNull(ui.frequencyGhz)
        assertNull(ui.maxFrequencyGhz)
        assertNull(ui.temperatureC)
        assertEquals(StatusLevel.Operational, ui.temperatureStatus)
    }

    @Test
    fun formatsSignedPercentWithOneDecimal() {
        assertEquals("+2.4%", CpuUi.formatSignedPercent(2.44f))
        assertEquals("-1.0%", CpuUi.formatSignedPercent(-1.0f))
        assertEquals("+0.0%", CpuUi.formatSignedPercent(0f))
        assertEquals("-0.3%", CpuUi.formatSignedPercent(-0.34f))
    }

    @Test
    fun derivesHistoryStatsAndTheBusiestCore() {
        val ui = CpuUi(
            cores = listOf(CpuCoreUi(0, 10f), CpuCoreUi(1, 90f)),
            usageHistory = listOf(10f, 30f, 20f),
        )

        assertEquals(20f, ui.usageHistoryAverage)
        assertEquals(30f, ui.usageHistoryPeak)
        assertEquals(1, ui.busiestCore?.index)
    }

    @Test
    fun overallStatusIsTheWorstOfUsageAndTemperature() {
        assertEquals(StatusLevel.Critical, CpuUi(usagePercent = 40f, temperatureC = 90f).overallStatus)
        assertEquals(StatusLevel.Degraded, CpuUi(usagePercent = 80f, temperatureC = null).overallStatus)
    }
}
