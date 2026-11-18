package io.buszi.nascope.ui.dashboard.customize

import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Alerts
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Gpu
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.SystemOverview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrickDragStateTest {

    private val moves = mutableListOf<Pair<DashboardBrickId, Int>>()
    private val order = listOf(SystemOverview, Alerts, Gpu)

    /** Rows of 40 px, except Alerts at 60 px (its "Not offered" line), stacked from 0. */
    private val dragState = BrickDragState { id, offset -> moves += id to offset }.apply {
        onRowPlaced(SystemOverview, top = 0f, height = 40)
        onRowPlaced(Alerts, top = 40f, height = 60)
        onRowPlaced(Gpu, top = 100f, height = 40)
    }

    @Test
    fun theRowFollowsTheDragUntilItsEdgeCrossesTheNeighboursMiddle() {
        dragState.start(SystemOverview, order)

        dragState.dragBy(30f)

        assertEquals(emptyList(), moves)
        assertEquals(30f, dragState.offset)
        assertEquals(30f..70f, dragState.draggedSpan())
    }

    @Test
    fun crossingTheNeighboursMiddleSwapsAndKeepsTheRowUnderTheFinger() {
        dragState.start(SystemOverview, order)

        dragState.dragBy(31f)

        assertEquals(listOf(SystemOverview to 1), moves)
        // Its place moved down by Alerts' 60 px; drawn where it was, 31 px below its old place.
        assertEquals(-29f, dragState.offset)
    }

    @Test
    fun aFastDragPassesSeveralRowsAtOnce() {
        dragState.start(Gpu, order)

        dragState.dragBy(-90f)

        assertEquals(listOf(Gpu to -1, Gpu to -1), moves)
        assertEquals(10f, dragState.offset)
    }

    @Test
    fun goingBackSwapsBackOnlyPastTheSameMiddle() {
        dragState.start(SystemOverview, order)
        dragState.dragBy(31f)

        dragState.dragBy(-1f)
        assertEquals(listOf(SystemOverview to 1), moves)

        dragState.dragBy(-1f)
        assertEquals(listOf(SystemOverview to 1, SystemOverview to -1), moves)
        assertEquals(29f, dragState.offset)
    }

    @Test
    fun theRowStopsAtTheEndsOfTheList() {
        dragState.start(SystemOverview, order)
        dragState.dragBy(-50f)
        assertEquals(0f, dragState.offset)

        dragState.dragBy(500f)
        assertEquals(listOf(SystemOverview to 1, SystemOverview to 1), moves)
        assertEquals(0f, dragState.offset)
    }

    @Test
    fun nothingMovesWithoutADragAndStopPutsTheRowBack() {
        dragState.dragBy(100f)
        assertEquals(emptyList(), moves)
        assertNull(dragState.draggedSpan())

        dragState.start(Alerts, order)
        dragState.dragBy(10f)
        dragState.stop()

        assertNull(dragState.draggedId)
        assertEquals(0f, dragState.offset)
    }
}
