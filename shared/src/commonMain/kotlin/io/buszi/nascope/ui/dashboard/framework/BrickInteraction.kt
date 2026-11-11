package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Drill-down state of a brick as seen from the dashboard grid.
 */
@Immutable
enum class BrickInteraction {
    /** The brick has no details; it is purely informational. */
    None,

    /** The brick has details that can be opened by tapping it. */
    Openable,

    /** The brick's details are currently open (visible beside the dashboard on wide windows). */
    Open,
}

/**
 * Provided by the dashboard grid around each brick so that the shared
 * [io.buszi.nascope.ui.dashboard.widgets.DashboardWidget] shell can render the
 * "details available" and "details open" chrome without every brick plumbing the state itself.
 */
val LocalBrickInteraction = compositionLocalOf { BrickInteraction.None }
