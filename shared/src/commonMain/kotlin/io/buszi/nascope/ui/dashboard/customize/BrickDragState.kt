package io.buszi.nascope.ui.dashboard.customize

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * The editor's drag: which row is dragged, how far it is drawn from its place, and when it passes a neighbour.
 *
 * The dragged row stays in its place in the layout and is drawn [offset] pixels away from it. When its leading edge
 * crosses a neighbour's middle the two swap places: [onMove] reports the step and [offset] shrinks by the neighbour's
 * height, so the row stays under the finger while its place moves. It keeps the order it works on to itself, so a
 * fast drag never decides on a list the screen has not caught up with.
 *
 * @param onMove The dragged brick passed a neighbour: `-1` up or `1` down.
 */
@Stable
class BrickDragState(private val onMove: (id: DashboardBrickId, offset: Int) -> Unit) {

    /** The brick being dragged, `null` when none is. */
    var draggedId: DashboardBrickId? by mutableStateOf(null)
        private set

    /** How far, in pixels, the dragged row is drawn from its place; positive is down. */
    var offset: Float by mutableFloatStateOf(0f)
        private set

    private val heights = mutableMapOf<DashboardBrickId, Int>()
    private val tops = mutableMapOf<DashboardBrickId, Float>()
    private var order: List<DashboardBrickId> = emptyList()

    /** Where a row's place is, in the scrolled content (unaffected by scrolling), and how tall it is, in pixels. */
    fun onRowPlaced(id: DashboardBrickId, top: Float, height: Int) {
        tops[id] = top
        heights[id] = height
    }

    /** The dragged row's top and bottom as drawn, in the scrolled content; `null` when nothing is dragged. */
    fun draggedSpan(): ClosedFloatingPointRange<Float>? {
        val id = draggedId ?: return null
        val top = (tops[id] ?: return null) + offset
        return top..top + (heights[id] ?: 0)
    }

    /** Starts dragging [id] among the rows in [order]. */
    fun start(id: DashboardBrickId, order: List<DashboardBrickId>) {
        this.order = order
        offset = 0f
        draggedId = id
    }

    /** Moves the dragged row by [delta] pixels, swapping it past every neighbour whose middle it crosses. */
    fun dragBy(delta: Float) {
        val id = draggedId ?: return
        var index = order.indexOf(id)
        if (index < 0) return
        offset += delta
        while (true) {
            val step = when {
                offset > 0 && index < order.lastIndex && offset > halfHeight(order[index + 1]) -> 1
                offset < 0 && index > 0 && -offset > halfHeight(order[index - 1]) -> -1
                else -> break
            }
            val neighbour = order[index + step]
            offset -= step * (heights[neighbour] ?: 0)
            order = order.toMutableList().apply { add(index + step, removeAt(index)) }
            index += step
            onMove(id, step)
        }
        // At an end there is nothing to pass: the row stops at its place instead of leaving the list.
        if (index == 0) offset = offset.coerceAtLeast(0f)
        if (index == order.lastIndex) offset = offset.coerceAtMost(0f)
    }

    /** Draws the dragged row [offset] pixels from its place, e.g. while it settles after a drop. */
    fun settleTo(offset: Float) {
        if (draggedId != null) this.offset = offset
    }

    /** Ends the drag; the row is drawn in its place again. */
    fun stop() {
        draggedId = null
        offset = 0f
    }

    private fun halfHeight(id: DashboardBrickId): Float = (heights[id] ?: Int.MAX_VALUE) / 2f
}
