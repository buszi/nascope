package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class GpuUiTest {

    @Test
    fun statusIsTheWorstOfUtilizationMemoryAndTemperature() {
        assertEquals(StatusLevel.Operational, gpu(util = 50f, memory = 50f, temp = 50f).status)
        assertEquals(StatusLevel.Degraded, gpu(util = 75f, memory = 10f, temp = 40f).status)
        assertEquals(StatusLevel.Degraded, gpu(util = 10f, memory = 80f, temp = 40f).status)
        assertEquals(StatusLevel.Critical, gpu(util = 10f, memory = 10f, temp = 85f).status)
        assertEquals(StatusLevel.Critical, gpu(util = 95f, memory = null, temp = null).status)
    }

    @Test
    fun aGpuThatReportsNothingIsNominal() {
        assertEquals(StatusLevel.Operational, gpu(util = null, memory = null, temp = null).status)
    }

    @Test
    fun overallStatusIsTheWorstGpuAndNominalWithNone() {
        assertEquals(StatusLevel.Operational, emptyList<GpuUi>().overallStatus)
        assertEquals(
            StatusLevel.Critical,
            listOf(gpu(util = 10f), gpu(util = 99f)).overallStatus,
        )
    }

    @Test
    fun fromMapsTheUsageAndTheHistory() {
        val data = ObserveGpuUseCase.Data(
            listOf(
                ObserveGpuUseCase.Gpu(GpuUsage("intel0", "Intel GPU", 12f, null, null, null), listOf(1f, 12f)),
            ),
        )

        assertEquals(
            listOf(GpuUi("intel0", "Intel GPU", 12f, null, null, null, listOf(1f, 12f))),
            GpuUi.from(data),
        )
    }

    private fun gpu(util: Float? = null, memory: Float? = null, temp: Float? = null) =
        GpuUi("id", "GPU", util, memory, temp, fanSpeedPercent = null)
}
