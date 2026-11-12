package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsBrick
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import kotlin.test.Test
import kotlin.test.assertEquals

class BrickVisibilityTest {

    private val bricks = BrickRegistry().getActiveBricks()

    @Test
    fun everyBrickIsShownWhilePluginsAreUnknown() {
        assertEquals(bricks, bricks.visibleWith(null))
    }

    @Test
    fun aBrickWithoutRequirementsIsAlwaysShown() {
        assertEquals(listOf(SystemOverviewBrick), listOf(SystemOverviewBrick).visibleWith(emptySet()))
    }

    @Test
    fun theGpuBrickNeedsTheGpuPlugin() {
        assertEquals(setOf("gpu"), GpuBrick.requiredPlugins)
        assertEquals(bricks - AlertsBrick, bricks.visibleWith(setOf("gpu")))
        assertEquals(bricks - GpuBrick - AlertsBrick, bricks.visibleWith(setOf("cpu")))
    }

    @Test
    fun theAlertsBrickNeedsTheAlertPlugin() {
        assertEquals(setOf("alert"), AlertsBrick.requiredPlugins)
        assertEquals(bricks - GpuBrick, bricks.visibleWith(setOf("alert")))
    }

    @Test
    fun aBrickNeedsAllItsPlugins() {
        val two = object : DashboardBrick by SystemOverviewBrick {
            override val requiredPlugins = setOf("a", "b")
        }
        assertEquals(emptyList(), listOf(two).visibleWith(setOf("a")))
        assertEquals(listOf(two), listOf(two).visibleWith(setOf("a", "b", "c")))
    }
}
