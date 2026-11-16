package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import kotlin.test.Test
import kotlin.test.assertEquals

/** The keys are stored in users' layouts: changing one loses that brick's place and switch. Never edit these. */
class DashboardBrickKeyTest {

    @Test
    fun everyBrickKeyIsPinned() {
        assertEquals(DashboardBrickKey("system"), DashboardBrickId.SystemOverview.key)
        assertEquals(DashboardBrickKey("cpu"), DashboardBrickId.CpuMatrix.key)
        assertEquals(DashboardBrickKey("gpu"), DashboardBrickId.Gpu.key)
        assertEquals(DashboardBrickKey("alerts"), DashboardBrickId.Alerts.key)
        assertEquals(DashboardBrickKey("memory"), DashboardBrickId.MemorySwap.key)
        assertEquals(DashboardBrickKey("storage"), DashboardBrickId.StoragePools.key)
        assertEquals(DashboardBrickKey("network"), DashboardBrickId.NetworkIo.key)
        assertEquals(DashboardBrickKey("sensors"), DashboardBrickId.SensorsThermals.key)
        assertEquals(DashboardBrickKey("containers"), DashboardBrickId.Containers.key)
        assertEquals(DashboardBrickKey("processes"), DashboardBrickId.ProcessList.key)
        assertEquals(10, DashboardBrickId.entries.size, "a new brick needs its key pinned above")
    }

    @Test
    fun everyBrickKeyIsDistinct() {
        val keys = DashboardBrickId.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun aBrickUsesTheKeyOfItsId() {
        BrickRegistry().allBricks.forEach { brick -> assertEquals(brick.id.key, brick.key) }
    }
}
