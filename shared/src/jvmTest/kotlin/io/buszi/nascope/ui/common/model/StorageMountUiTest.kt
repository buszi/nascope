package io.buszi.nascope.ui.common.model

import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageMountUiTest {

    @Test
    fun usageRatioIsUsedOverTotal() {
        assertEquals(0.25f, mount(total = 400L, used = 100L).usageRatio)
    }

    @Test
    fun usageRatioIsZeroForEmptyCapacity() {
        assertEquals(0f, mount(total = 0L, used = 0L).usageRatio)
    }

    @Test
    fun statusFollowsUsageThresholds() {
        assertEquals(StatusLevel.Operational, mount(total = 100L, used = 79L).status)
        assertEquals(StatusLevel.Degraded, mount(total = 100L, used = 80L).status)
        assertEquals(StatusLevel.Critical, mount(total = 100L, used = 90L).status)
    }

    @Test
    fun unhealthyMountIsCriticalRegardlessOfUsage() {
        assertEquals(StatusLevel.Critical, mount(total = 100L, used = 1L, isHealthy = false).status)
    }

    @Test
    fun overallStatusIsWorstMountStatus() {
        assertEquals(StatusLevel.Operational, emptyList<StorageMountUi>().overallStatus)
        assertEquals(
            StatusLevel.Degraded,
            listOf(mount(100L, 10L), mount(100L, 85L)).overallStatus,
        )
        assertEquals(
            StatusLevel.Critical,
            listOf(mount(100L, 85L), mount(100L, 95L)).overallStatus,
        )
    }

    private fun mount(total: Long, used: Long, isHealthy: Boolean = true) = StorageMountUi(
        mountPoint = "/",
        deviceName = "/dev/sda1",
        fsType = "ext4",
        totalBytes = total,
        usedBytes = used,
        freeBytes = total - used,
        isHealthy = isHealthy,
    )
}
