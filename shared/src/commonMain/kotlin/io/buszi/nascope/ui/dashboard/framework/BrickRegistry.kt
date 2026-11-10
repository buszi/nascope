package io.buszi.nascope.ui.dashboard.framework

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
    private val bricks: List<DashboardBrick> = listOf(SystemOverviewBrick)

    fun getActiveBricks(): List<DashboardBrick> = bricks
}
