package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.arrangedBy
import io.buszi.nascope.ui.dashboard.framework.enabledIn
import io.buszi.nascope.ui.dashboard.framework.visibleWith

/**
 * What the dashboard editor shows: a row per registered brick, in the layout's order, whether turned on or not.
 *
 * Edits ([withEnabled], [moved]) are functions returning a new value; [toLayout] turns the rows back into the complete
 * layout to store.
 *
 * @property rows Every registered brick, first to last.
 * @property isCustomized Whether the stored layout differs from [DashboardLayout.Default], so "Reset to default" has
 * something to do.
 */
@Immutable
data class DashboardCustomizeUi(
    val rows: List<BrickRow> = emptyList(),
    val isCustomized: Boolean = false,
) {

    /** How many bricks are turned on. */
    val enabledCount: Int get() = rows.count { it.isEnabled }

    /** The same rows with the brick [id] turned on or off; it keeps its place. An unknown [id] changes nothing. */
    fun withEnabled(id: DashboardBrickId, enabled: Boolean): DashboardCustomizeUi =
        copy(rows = rows.map { if (it.id == id) it.copy(isEnabled = enabled) else it })

    /**
     * The same rows with the one at [from] moved to [to], the rows between shifting by one. Every row counts, turned
     * off or not offered, since the rows are the whole order. An index outside the rows changes nothing.
     */
    fun moved(from: Int, to: Int): DashboardCustomizeUi {
        if (from == to || from !in rows.indices || to !in rows.indices) return this
        val reordered = rows.toMutableList().apply { add(to, removeAt(from)) }
        return copy(rows = reordered)
    }

    /**
     * The same rows, each with its own state, in the order of the bricks in [order]. Rows [order] does not name keep
     * their relative order after the named ones.
     */
    fun inOrderOf(order: List<DashboardBrickId>): DashboardCustomizeUi {
        val position = order.withIndex().associate { (index, id) -> id to index }
        return copy(rows = rows.sortedBy { position[it.id] ?: Int.MAX_VALUE })
    }

    /** Whether the row at [index] has a row above it to swap with. */
    fun canMoveUp(index: Int): Boolean = index in 1..rows.lastIndex

    /** Whether the row at [index] has a row below it to swap with. */
    fun canMoveDown(index: Int): Boolean = index in 0..<rows.lastIndex

    /** The complete layout: every row's key in the rows' order, and the rows turned off as hidden. */
    fun toLayout(): DashboardLayout = DashboardLayout(
        order = rows.map { it.id.key },
        hidden = rows.filterNot { it.isEnabled }.mapTo(mutableSetOf()) { it.id.key },
    )

    /**
     * One brick in the editor.
     *
     * @property isEnabled Whether the brick is turned on. A brick that is off keeps its place, dimmed.
     * @property isOffered `false` only when the server's plugins are known and lack one the brick needs; the switch
     * still works, the dashboard just cannot show the brick on this server.
     */
    @Immutable
    data class BrickRow(
        val id: DashboardBrickId,
        val isEnabled: Boolean = true,
        val isOffered: Boolean = true,
    ) {
        /** The brick's stable layout key, for test tags. */
        val key: String get() = id.key.value
    }

    companion object {
        /**
         * The editor's rows for [bricks] (every registered brick, in registry order) arranged by [layout], with the
         * switches from [DashboardLayout.hidden] and the availability line from [availablePlugins] (`null`: unknown,
         * so every brick counts as offered).
         */
        fun from(
            bricks: List<DashboardBrick>,
            layout: DashboardLayout,
            availablePlugins: Set<String>?,
        ): DashboardCustomizeUi {
            // The dashboard's own rules, so the editor never disagrees with what the dashboard shows.
            val enabled = bricks.enabledIn(layout).mapTo(mutableSetOf()) { it.id }
            val offered = bricks.visibleWith(availablePlugins).mapTo(mutableSetOf()) { it.id }
            return DashboardCustomizeUi(
                rows = bricks.arrangedBy(layout).map { brick ->
                    BrickRow(id = brick.id, isEnabled = brick.id in enabled, isOffered = brick.id in offered)
                },
                isCustomized = layout != DashboardLayout.Default,
            )
        }
    }
}
