package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixBrick
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapBrick
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoBrick
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsBrick
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import org.koin.core.annotation.Single

/**
 * Registry providing the set of available and active dashboard bricks.
 *
 * For now, this acts as a mock provider supplying the active list of bricks.
 * In later milestones, this will integrate with user-configured layout persistence.
 */
@Single
class BrickRegistry {
    private val bricks: List<DashboardBrick> = listOf(
        SystemOverviewBrick,
        CpuMatrixBrick,
        MemorySwapBrick,
        StoragePoolsBrick,
        NetworkIoBrick,
        SensorsThermalsBrick,
    )

    fun getActiveBricks(): List<DashboardBrick> = bricks
}
