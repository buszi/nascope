package io.buszi.nascope.ui.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DashboardSplitSceneStrategyTest {

    private val scope = SceneStrategyScope<NavKey>()

    private val dashboardEntry = NavEntry<NavKey>(
        key = NavRoute.Dashboard,
        metadata = DashboardSplitSceneStrategy.mainPane(),
    ) {}

    private val detailsEntry = NavEntry<NavKey>(
        key = NavRoute.Details(DashboardBrickId.CpuMatrix),
        metadata = DashboardSplitSceneStrategy.detailPane(),
    ) {}

    private val nodesEntry = NavEntry<NavKey>(
        key = NavRoute.Nodes,
    ) {}

    @Test
    fun returnsSplitSceneOnExpandedWidthWhenDashboardAndDetailsAreStacked() {
        val strategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Expanded)
        val entries = listOf(dashboardEntry, detailsEntry)

        val scene = with(strategy) { scope.calculateScene(entries) }

        val splitScene = assertIs<DashboardSplitScene<NavKey>>(scene)
        assertEquals(listOf(dashboardEntry, detailsEntry), splitScene.entries)
        assertEquals(listOf(dashboardEntry), splitScene.previousEntries)
    }

    @Test
    fun returnsNullOnCompactAndMediumWidthsForSinglePaneFallback() {
        val entries = listOf(dashboardEntry, detailsEntry)

        val compactStrategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Compact)
        val mediumStrategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Medium)

        assertNull(with(compactStrategy) { scope.calculateScene(entries) })
        assertNull(with(mediumStrategy) { scope.calculateScene(entries) })
    }

    @Test
    fun returnsNullWhenTopEntryIsNotDetailPane() {
        val strategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Expanded)

        assertNull(with(strategy) { scope.calculateScene(listOf(dashboardEntry)) })
        assertNull(with(strategy) { scope.calculateScene(listOf(dashboardEntry, nodesEntry)) })
    }
}
