package io.buszi.nascope.ui.dashboard.framework

import io.buszi.nascope.domain.model.dashboard.DashboardLayout

/**
 * Every registered brick (the receiver, in registry order), in the [layout]'s order.
 *
 * - Bricks named in [DashboardLayout.order] come in that order; a key named twice counts where it is first named.
 * - A brick the order does not name (added by a later app version, or every brick when the order is empty) goes
 *   right after the nearest brick before it in the registry order that the result already holds, or first when
 *   there is none. A new brick so appears beside its designed neighbours, and [DashboardLayout.Default] gives exactly
 *   the registry order.
 * - A key no brick has (a brick removed later, a downgrade) is skipped.
 */
fun List<DashboardBrick>.arrangedBy(layout: DashboardLayout): List<DashboardBrick> {
    val byKey = associateBy { it.key }
    val result = layout.order.distinct().mapNotNull { byKey[it] }.toMutableList()
    val placed = result.mapTo(mutableSetOf()) { it.key }

    forEachIndexed { index, brick ->
        if (brick.key in placed) return@forEachIndexed
        val predecessor = (index - 1 downTo 0).firstOrNull { this[it].key in placed }?.let { this[it] }
        val position = if (predecessor == null) 0 else result.indexOf(predecessor) + 1
        result.add(position, brick)
        placed += brick.key
    }
    return result
}

/** The bricks the user has not turned off; a brick [DashboardLayout.hidden] does not name is on. */
fun List<DashboardBrick>.enabledIn(layout: DashboardLayout): List<DashboardBrick> =
    filterNot { it.key in layout.hidden }
