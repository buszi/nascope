package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi.BrickRow
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Alerts
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Gpu
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.ProcessList
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.SystemOverview
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsBrick
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardCustomizeUiTest {

    private val registry = BrickRegistry().allBricks
    private val bricks = listOf(SystemOverviewBrick, AlertsBrick, GpuBrick)

    private fun layout(vararg order: DashboardBrickId, hidden: Set<DashboardBrickId> = emptySet()) = DashboardLayout(
        order = order.map { it.key },
        hidden = hidden.mapTo(mutableSetOf()) { it.key },
    )

    @Test
    fun theDefaultLayoutGivesEveryRegisteredBrickOnInTheRegistryOrder() {
        val ui = DashboardCustomizeUi.from(registry, DashboardLayout.Default, availablePlugins = null)

        assertEquals(registry.map { BrickRow(id = it.id) }, ui.rows)
        assertEquals(registry.size, ui.enabledCount)
        assertFalse(ui.isCustomized)
    }

    @Test
    fun rowsFollowTheLayoutsOrderAndOffBricksKeepTheirPlace() {
        val ui = DashboardCustomizeUi.from(bricks, layout(Gpu, SystemOverview, Alerts, hidden = setOf(SystemOverview)), null)

        assertEquals(
            listOf(
                BrickRow(id = Gpu),
                BrickRow(id = SystemOverview, isEnabled = false),
                BrickRow(id = Alerts),
            ),
            ui.rows,
        )
        assertEquals(2, ui.enabledCount)
        assertTrue(ui.isCustomized)
    }

    @Test
    fun aBrickTheLayoutDoesNotNameGetsARowAndUnknownKeysGetNone() {
        val stored = DashboardLayout(
            order = listOf(DashboardBrickKey("removed"), Gpu.key, SystemOverview.key),
            hidden = setOf(DashboardBrickKey("removed")),
        )

        val ui = DashboardCustomizeUi.from(bricks, stored, null)

        assertEquals(listOf(Gpu, SystemOverview, Alerts), ui.rows.map { it.id })
        assertTrue(ui.rows.all { it.isEnabled })
    }

    @Test
    fun theAvailabilityLineShowsOnlyWhenThePluginsAreKnownAndLackTheBricks() {
        val unknown = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, availablePlugins = null)
        val withoutGpu = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, availablePlugins = setOf("alert"))
        val withAll = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, availablePlugins = setOf("alert", "gpu"))
        val none = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, availablePlugins = emptySet())

        assertTrue(unknown.rows.all { it.isOffered })
        assertEquals(listOf(Gpu), withoutGpu.rows.filterNot { it.isOffered }.map { it.id })
        assertTrue(withAll.rows.all { it.isOffered })
        assertEquals(listOf(Alerts, Gpu), none.rows.filterNot { it.isOffered }.map { it.id })
    }

    @Test
    fun anUnofferedBrickKeepsItsSwitch() {
        val ui = DashboardCustomizeUi.from(bricks, layout(hidden = setOf(Gpu)), availablePlugins = emptySet())

        assertEquals(BrickRow(id = Gpu, isEnabled = false, isOffered = false), ui.rows.single { it.id == Gpu })
        assertEquals(BrickRow(id = Gpu, isEnabled = true, isOffered = false), ui.withEnabled(Gpu, true).rows.single { it.id == Gpu })
    }

    @Test
    fun withEnabledTurnsABrickOffAndBackOnInItsPlace() {
        val ui = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, null)

        val off = ui.withEnabled(Alerts, false)
        val on = off.withEnabled(Alerts, true)

        assertEquals(listOf(true, false, true), off.rows.map { it.isEnabled })
        assertEquals(ui.rows.map { it.id }, off.rows.map { it.id })
        assertEquals(ui.rows, on.rows)
    }

    @Test
    fun withEnabledForABrickWithoutARowChangesNothing() {
        val ui = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, null)

        assertEquals(ui, ui.withEnabled(ProcessList, false))
    }

    @Test
    fun movedTakesARowUpAndDownShiftingTheRowsBetween() {
        val ui = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, null)

        assertEquals(listOf(SystemOverview, Gpu, Alerts), ui.moved(from = 2, to = 1).rows.map { it.id })
        assertEquals(listOf(Alerts, SystemOverview, Gpu), ui.moved(from = 0, to = 1).rows.map { it.id })
        assertEquals(listOf(Gpu, SystemOverview, Alerts), ui.moved(from = 2, to = 0).rows.map { it.id })
        assertEquals(listOf(Alerts, Gpu, SystemOverview), ui.moved(from = 0, to = 2).rows.map { it.id })
    }

    @Test
    fun movedToTheSamePlaceOrOutsideTheRowsChangesNothing() {
        val ui = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, null)

        assertEquals(ui, ui.moved(from = 1, to = 1))
        assertEquals(ui, ui.moved(from = 0, to = -1))
        assertEquals(ui, ui.moved(from = 2, to = 3))
        assertEquals(ui, ui.moved(from = -1, to = 0))
        assertEquals(ui, ui.moved(from = 3, to = 2))
    }

    @Test
    fun movedCarriesARowThatIsOffOrNotOfferedWithItsState() {
        val ui = DashboardCustomizeUi.from(bricks, layout(hidden = setOf(Gpu)), availablePlugins = setOf("alert"))

        val moved = ui.moved(from = 2, to = 0)

        assertEquals(BrickRow(id = Gpu, isEnabled = false, isOffered = false), moved.rows.first())
        assertEquals(
            DashboardLayout(order = listOf(Gpu.key, SystemOverview.key, Alerts.key), hidden = setOf(Gpu.key)),
            moved.toLayout(),
        )
    }

    @Test
    fun inOrderOfKeepsEveryRowsStateAndPutsUnnamedRowsLast() {
        val ui = DashboardCustomizeUi.from(bricks, layout(hidden = setOf(Alerts)), availablePlugins = setOf("alert"))

        val reordered = ui.inOrderOf(listOf(Gpu, Alerts))

        assertEquals(
            listOf(
                BrickRow(id = Gpu, isOffered = false),
                BrickRow(id = Alerts, isEnabled = false),
                BrickRow(id = SystemOverview),
            ),
            reordered.rows,
        )
        assertEquals(ui, ui.inOrderOf(ui.rows.map { it.id }))
    }

    @Test
    fun onlyTheFirstRowCannotMoveUpAndOnlyTheLastCannotMoveDown() {
        val ui = DashboardCustomizeUi.from(bricks, DashboardLayout.Default, null)

        assertEquals(listOf(false, true, true), ui.rows.indices.map(ui::canMoveUp))
        assertEquals(listOf(true, true, false), ui.rows.indices.map(ui::canMoveDown))
        assertFalse(ui.canMoveUp(3))
        assertFalse(ui.canMoveDown(-1))
        assertFalse(DashboardCustomizeUi(rows = listOf(BrickRow(Gpu))).let { it.canMoveUp(0) || it.canMoveDown(0) })
    }

    @Test
    fun toLayoutListsEveryRowsKeyInOrderAndTheOffOnesAsHidden() {
        val ui = DashboardCustomizeUi.from(bricks, layout(Gpu, SystemOverview, Alerts), null).withEnabled(Alerts, false)

        assertEquals(
            DashboardLayout(order = listOf(Gpu.key, SystemOverview.key, Alerts.key), hidden = setOf(Alerts.key)),
            ui.toLayout(),
        )
    }

    @Test
    fun theDefaultLayoutWrittenBackListsEveryRegisteredKey() {
        val written = DashboardCustomizeUi.from(registry, DashboardLayout.Default, null).toLayout()

        assertEquals(registry.map { it.key }, written.order)
        assertEquals(emptySet(), written.hidden)
    }
}
