package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsBrick
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersBrick
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixBrick
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapBrick
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoBrick
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListBrick
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsBrick
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import org.koin.core.annotation.Single

/**
 * Every dashboard brick this app version has.
 *
 * The list order is the default dashboard order: it is what [DashboardLayout.Default] shows, and where a brick the
 * stored layout does not know yet is placed (see [arrangedBy]). Which bricks are shown, and in what order, is decided
 * by `DashboardViewModel` from the stored layout and the server's plugins.
 */
@Single
class BrickRegistry {

    /** Every registered brick, in the default order. */
    val allBricks: List<DashboardBrick> = listOf(
        SystemOverviewBrick,
        AlertsBrick,
        CpuMatrixBrick,
        GpuBrick,
        MemorySwapBrick,
        StoragePoolsBrick,
        NetworkIoBrick,
        SensorsThermalsBrick,
        ContainersBrick,
        ProcessListBrick,
    )

    /** Returns the brick registered under [id], whether it is turned on or not, or `null` if there is none. */
    fun findBrick(id: DashboardBrickId): DashboardBrick? = allBricks.firstOrNull { it.id == id }
}
