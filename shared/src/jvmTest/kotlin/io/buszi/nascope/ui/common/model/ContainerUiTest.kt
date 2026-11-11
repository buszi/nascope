package io.buszi.nascope.ui.common.model

import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContainerUiTest {

    @Test
    fun stateStatusReflectsSeverity() {
        assertEquals(StatusLevel.Operational, ContainerStateUi.Running.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Paused.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Stopped.status)
        assertEquals(StatusLevel.Critical, ContainerStateUi.Restarting.status)
    }

    @Test
    fun memoryRatioIsUsageOverLimit() {
        assertEquals(0.25f, container(usage = 25L, limit = 100L).memoryRatio)
    }

    @Test
    fun memoryRatioIsClampedToUnitRange() {
        assertEquals(1f, container(usage = 300L, limit = 100L).memoryRatio)
    }

    @Test
    fun memoryRatioIsNullWithoutLimit() {
        assertNull(container(usage = 25L, limit = null).memoryRatio)
        assertNull(container(usage = 25L, limit = 0L).memoryRatio)
    }

    private fun container(usage: Long, limit: Long?) = ContainerUi(
        id = "id",
        name = "name",
        image = "image",
        state = ContainerStateUi.Running,
        cpuPercent = 1f,
        memoryUsageBytes = usage,
        memoryLimitBytes = limit,
        uptime = "Up",
    )
}
