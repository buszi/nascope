package io.buszi.nascope.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NascopeNavigatorTest {

    private fun createNavigator(initialRoute: NavRoute = NavRoute.Dashboard): NascopeNavigator =
        NascopeNavigator(backStack = NavBackStack(initialRoute))

    @Test
    fun startsAtDashboardWithBackDisabled() {
        val navigator = createNavigator()

        assertEquals(NavRoute.Dashboard, navigator.currentRoute)
        assertEquals(TopLevelDestination.Dashboard, navigator.activeTopLevelDestination)
        assertNull(navigator.selectedBrickId)
        assertFalse(navigator.canNavigateBack)
        assertFalse(navigator.navigateBack())
        assertEquals(listOf<NavKey>(NavRoute.Dashboard), navigator.backStack.toList())
    }

    @Test
    fun navigatingBetweenTopLevelTabsPreservesSingleDashboardRoot() {
        val navigator = createNavigator()

        navigator.navigateToTopLevel(TopLevelDestination.Nodes)
        assertEquals(NavRoute.Nodes, navigator.currentRoute)
        assertEquals(TopLevelDestination.Nodes, navigator.activeTopLevelDestination)
        assertTrue(navigator.canNavigateBack)
        assertEquals(
            listOf<NavKey>(NavRoute.Dashboard, NavRoute.Nodes),
            navigator.backStack.toList(),
        )

        navigator.navigateToTopLevel(TopLevelDestination.Settings)
        assertEquals(NavRoute.Settings, navigator.currentRoute)
        assertEquals(TopLevelDestination.Settings, navigator.activeTopLevelDestination)
        assertEquals(
            listOf<NavKey>(NavRoute.Dashboard, NavRoute.Settings),
            navigator.backStack.toList(),
        )

        assertTrue(navigator.navigateBack())
        assertEquals(NavRoute.Dashboard, navigator.currentRoute)
        assertFalse(navigator.canNavigateBack)
    }

    @Test
    fun navigatingToDetailsAndSwitchingBricksReplacesTopDetailEntry() {
        val navigator = createNavigator()

        navigator.navigateToDetails(DashboardBrickId.CpuMatrix)
        assertEquals(NavRoute.Details(DashboardBrickId.CpuMatrix), navigator.currentRoute)
        assertEquals(TopLevelDestination.Dashboard, navigator.activeTopLevelDestination)
        assertEquals(DashboardBrickId.CpuMatrix, navigator.selectedBrickId)
        assertTrue(navigator.canNavigateBack)
        assertEquals(
            listOf<NavKey>(NavRoute.Dashboard, NavRoute.Details(DashboardBrickId.CpuMatrix)),
            navigator.backStack.toList(),
        )

        navigator.navigateToDetails(DashboardBrickId.StoragePools)
        assertEquals(NavRoute.Details(DashboardBrickId.StoragePools), navigator.currentRoute)
        assertEquals(DashboardBrickId.StoragePools, navigator.selectedBrickId)
        assertEquals(
            listOf<NavKey>(NavRoute.Dashboard, NavRoute.Details(DashboardBrickId.StoragePools)),
            navigator.backStack.toList(),
        )

        navigator.navigateToTopLevel(TopLevelDestination.Dashboard)
        assertEquals(NavRoute.Dashboard, navigator.currentRoute)
        assertNull(navigator.selectedBrickId)
        assertFalse(navigator.canNavigateBack)
    }
}
