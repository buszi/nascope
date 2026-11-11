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
    fun returnsSplitSceneOnExpandedAndMediumWidthsWhenDashboardAndDetailsAreStacked() {
        val entries = listOf(dashboardEntry, detailsEntry)

        for (widthClass in listOf(WindowWidthClass.Expanded, WindowWidthClass.Medium)) {
            val strategy = DashboardSplitSceneStrategy<NavKey>(widthClass)
            val scene = with(strategy) { scope.calculateScene(entries) }

            val splitScene = assertIs<DashboardSplitScene<NavKey>>(scene)
            assertEquals(dashboardEntry.contentKey, splitScene.key)
            assertEquals(listOf(dashboardEntry, detailsEntry), splitScene.entries)
            assertEquals(listOf(dashboardEntry), splitScene.previousEntries)
        }
    }

    @Test
    fun returnsSplitSceneWithSameKeyWhenDashboardIsAloneOnNonCompactWidths() {
        val strategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Expanded)

        val dashboardOnlyScene = assertIs<DashboardSplitScene<NavKey>>(
            with(strategy) { scope.calculateScene(listOf(dashboardEntry)) },
        )
        val withDetailsScene = assertIs<DashboardSplitScene<NavKey>>(
            with(strategy) { scope.calculateScene(listOf(dashboardEntry, detailsEntry)) },
        )

        assertEquals(dashboardOnlyScene.key, withDetailsScene.key)
        assertEquals(listOf(dashboardEntry), dashboardOnlyScene.entries)
        assertEquals(emptyList(), dashboardOnlyScene.previousEntries)
        assertNull(dashboardOnlyScene.detailEntry)
    }

    @Test
    fun returnsNullOnCompactWidthForSinglePaneFallback() {
        val entries = listOf(dashboardEntry, detailsEntry)
        val compactStrategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Compact)

        assertNull(with(compactStrategy) { scope.calculateScene(listOf(dashboardEntry)) })
        assertNull(with(compactStrategy) { scope.calculateScene(entries) })
    }

    @Test
    fun returnsNullWhenTopEntryIsNotDashboardOrDetailPane() {
        val strategy = DashboardSplitSceneStrategy<NavKey>(WindowWidthClass.Expanded)

        assertNull(with(strategy) { scope.calculateScene(listOf(dashboardEntry, nodesEntry)) })
    }
}
