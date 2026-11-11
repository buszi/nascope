package io.buszi.nascope.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * State-driven Navigation 3 backstack controller for Nascope.
 *
 * Manages top-level destination switching (`Dashboard`, `Nodes`, `Settings`),
 * contextual brick detail drill-downs (`Details(brickId)`), and back navigation
 * while keeping [NavRoute.Dashboard] anchored at the root of the backstack.
 */
@Stable
class NascopeNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    /**
     * Currently active [NavRoute] at the top of the [backStack].
     */
    val currentRoute: NavRoute
        get() = backStack.lastOrNull() as? NavRoute ?: NavRoute.Dashboard

    /**
     * Currently active [TopLevelDestination] highlighted in the navigation chrome.
     */
    val activeTopLevelDestination: TopLevelDestination
        get() = currentRoute.toTopLevelDestination()

    /**
     * Currently selected [DashboardBrickId] if [NavRoute.Details] is active, or `null`.
     */
    val selectedBrickId: DashboardBrickId?
        get() = (currentRoute as? NavRoute.Details)?.brickId

    /**
     * Whether there is more than one destination on the [backStack] that can be popped.
     */
    val canNavigateBack: Boolean
        get() = backStack.size > 1

    /**
     * Navigates to a top-level destination ([TopLevelDestination.Dashboard], [TopLevelDestination.Nodes], [TopLevelDestination.Settings]).
     *
     * Keeps [NavRoute.Dashboard] at the root of the backstack so back navigation from secondary
     * top-level tabs always returns cleanly to the primary telemetry cockpit without accumulating
     * duplicate tab entries.
     */
    fun navigateToTopLevel(destination: TopLevelDestination) {
        when (destination) {
            TopLevelDestination.Dashboard -> {
                if (backStack.size == 1 && backStack.first() == NavRoute.Dashboard) return
                backStack.clear()
                backStack.add(NavRoute.Dashboard)
            }
            TopLevelDestination.Nodes,
            TopLevelDestination.Settings,
            -> {
                val targetRoute = destination.route
                if (currentRoute == targetRoute) return
                backStack.clear()
                backStack.add(NavRoute.Dashboard)
                backStack.add(targetRoute)
            }
        }
    }

    /**
     * Opens or updates the detail route for [brickId].
     *
     * If a [NavRoute.Details] entry is already at the top of the stack, replaces it in-place
     * so switching inspected bricks in multi-pane mode does not stack redundant detail screens.
     */
    fun navigateToDetails(brickId: DashboardBrickId) {
        val target = NavRoute.Details(brickId)
        val current = currentRoute
        if (current == target) return

        if (current is NavRoute.Details && backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = target
        } else {
            if (backStack.isEmpty() || backStack.first() != NavRoute.Dashboard) {
                backStack.clear()
                backStack.add(NavRoute.Dashboard)
            }
            backStack.add(target)
        }
    }

    /**
     * Pops the top entry off the [backStack] if more than one entry is present.
     *
     * @return `true` if an entry was popped, `false` if already at the root destination.
     */
    fun navigateBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

/**
 * Remembers and saves a [NascopeNavigator] backed by Navigation 3's [rememberNavBackStack].
 */
@Composable
fun rememberNascopeNavigator(
    initialRoute: NavRoute = NavRoute.Dashboard,
): NascopeNavigator {
    val backStack = rememberNavBackStack(
        configuration = NavRouteSavedStateConfiguration,
        initialRoute,
    )
    return remember(backStack) {
        NascopeNavigator(backStack = backStack)
    }
}
