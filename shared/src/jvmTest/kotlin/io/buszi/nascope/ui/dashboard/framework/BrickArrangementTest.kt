package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
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
import kotlin.test.Test
import kotlin.test.assertEquals

class BrickArrangementTest {

    private val registry = BrickRegistry().allBricks

    private fun layout(vararg order: String, hidden: Set<String> = emptySet()) = DashboardLayout(
        order = order.map(::DashboardBrickKey),
        hidden = hidden.mapTo(mutableSetOf(), ::DashboardBrickKey),
    )

    private fun List<DashboardBrick>.keys() = map { it.key.value }

    @Test
    fun theRegistryOrderIsTheDefaultOrder() {
        assertEquals(
            listOf(
                SystemOverviewBrick, AlertsBrick, CpuMatrixBrick, GpuBrick, MemorySwapBrick,
                StoragePoolsBrick, NetworkIoBrick, SensorsThermalsBrick, ContainersBrick, ProcessListBrick,
            ),
            registry,
        )
    }

    @Test
    fun theDefaultLayoutGivesTheRegistryOrder() {
        assertEquals(registry, registry.arrangedBy(DashboardLayout.Default))
    }

    @Test
    fun aFullCustomOrderIsFollowed() {
        val order = registry.keys().reversed()

        assertEquals(order, registry.arrangedBy(layout(*order.toTypedArray())).keys())
    }

    @Test
    fun aBrickTheOrderDoesNotNameGoesRightAfterItsRegistryPredecessor() {
        // An order stored before the GPU brick existed, with the CPU brick moved to the end.
        val arranged = registry.arrangedBy(
            layout("system", "alerts", "memory", "storage", "network", "sensors", "containers", "processes", "cpu"),
        )

        assertEquals(
            listOf("system", "alerts", "memory", "storage", "network", "sensors", "containers", "processes", "cpu", "gpu"),
            arranged.keys(),
        )
    }

    @Test
    fun neighbouringNewBricksKeepTheirRegistryOrder() {
        val arranged = registry.arrangedBy(
            layout("processes", "system", "alerts", "memory", "storage", "network", "sensors", "containers"),
        )

        assertEquals(
            listOf("processes", "system", "alerts", "cpu", "gpu", "memory", "storage", "network", "sensors", "containers"),
            arranged.keys(),
        )
    }

    @Test
    fun aNewBrickThatIsFirstInTheRegistryGoesFirst() {
        val arranged = registry.arrangedBy(
            layout("processes", "alerts", "cpu", "gpu", "memory", "storage", "network", "sensors", "containers"),
        )

        assertEquals(
            listOf("system", "processes", "alerts", "cpu", "gpu", "memory", "storage", "network", "sensors", "containers"),
            arranged.keys(),
        )
    }

    @Test
    fun unknownKeysAreSkipped() {
        val arranged = registry.arrangedBy(layout("raid", "processes", "ups", "system"))

        assertEquals(
            listOf("processes", "system", "alerts", "cpu", "gpu", "memory", "storage", "network", "sensors", "containers"),
            arranged.keys(),
        )
    }

    @Test
    fun aKeyNamedTwiceCountsWhereItIsFirstNamed() {
        // The bricks the order does not name follow their registry predecessor: alerts after system, gpu after cpu.
        val arranged = registry.arrangedBy(layout("cpu", "system", "cpu", "system"))

        assertEquals(
            listOf("cpu", "gpu", "memory", "storage", "network", "sensors", "containers", "processes", "system", "alerts"),
            arranged.keys(),
        )
    }

    @Test
    fun everyRegisteredBrickIsArrangedExactlyOnce() {
        listOf(
            DashboardLayout.Default,
            layout("processes"),
            layout("gpu", "gpu", "nope", "system"),
            layout(*registry.keys().shuffled(kotlin.random.Random(7)).toTypedArray()),
        ).forEach { layout ->
            assertEquals(registry.toSet(), registry.arrangedBy(layout).toSet(), "$layout")
            assertEquals(registry.size, registry.arrangedBy(layout).size, "$layout")
        }
    }

    @Test
    fun everyBrickIsEnabledByDefault() {
        assertEquals(registry, registry.enabledIn(DashboardLayout.Default))
    }

    @Test
    fun hiddenBricksAreNotEnabledAndUnknownHiddenKeysAreIgnored() {
        val enabled = registry.enabledIn(layout(hidden = setOf("gpu", "processes", "ups")))

        assertEquals(registry - GpuBrick - ProcessListBrick, enabled)
    }

    @Test
    fun aHiddenBrickIsOffAndEveryOtherBrickIsOnWhetherTheOrderNamesItOrNot() {
        val layout = layout("system", hidden = setOf("system"))

        assertEquals(registry - SystemOverviewBrick, registry.arrangedBy(layout).enabledIn(layout))
    }

    @Test
    fun aHiddenBrickStaysHiddenWhenTheServerOffersItsPlugins() {
        val layout = layout(hidden = setOf("gpu"))

        val shown = registry.arrangedBy(layout).enabledIn(layout).visibleWith(setOf("gpu", "alert"))

        assertEquals(registry - GpuBrick, shown)
    }

    @Test
    fun anEnabledBrickIsNotShownWhenTheServerLacksItsPlugins() {
        val shown = registry.arrangedBy(DashboardLayout.Default).enabledIn(DashboardLayout.Default).visibleWith(setOf("alert"))

        assertEquals(registry - GpuBrick, shown)
    }

    @Test
    fun whileThePluginsAreUnknownEveryEnabledBrickIsShownInTheLayoutsOrder() {
        val layout = layout("processes", "gpu", hidden = setOf("alerts"))

        val shown = registry.arrangedBy(layout).enabledIn(layout).visibleWith(null)

        assertEquals(
            listOf("system", "cpu", "processes", "gpu", "memory", "storage", "network", "sensors", "containers"),
            shown.keys(),
        )
    }
}
