package io.buszi.nascope.ui.dashboard.framework

/**
 * The bricks to show given the plugins the server offers: a brick is visible when all its
 * [DashboardBrick.requiredPlugins] are available. `null` (unknown, e.g. the plugin list is still loading or failed)
 * shows everything, so a slow `pluginslist` never blanks the dashboard.
 */
fun List<DashboardBrick>.visibleWith(availablePlugins: Set<String>?): List<DashboardBrick> =
    if (availablePlugins == null) this else filter { availablePlugins.containsAll(it.requiredPlugins) }
