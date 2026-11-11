package io.buszi.nascope.ui.dashboard

import androidx.compose.ui.unit.dp
import io.buszi.nascope.ui.common.theme.AppSpacing
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.framework.BrickHeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardGridMetricsTest {

    private val spacing = AppSpacing()

    @Test
    fun compactUsesTwoColumnsWithPhoneMarginAndGutter() {
        val metrics = DashboardGridMetrics.forWidthClass(WindowWidthClass.Compact, spacing)

        assertEquals(2, metrics.columns)
        assertEquals(12.dp, metrics.gutter)
        assertEquals(16.dp, metrics.margin)
        assertFalse(metrics.fixedBrickHeights)
    }

    @Test
    fun mediumUsesThreeColumnsWithTabletMarginAndGutter() {
        val metrics = DashboardGridMetrics.forWidthClass(WindowWidthClass.Medium, spacing)

        assertEquals(3, metrics.columns)
        assertEquals(16.dp, metrics.gutter)
        assertEquals(24.dp, metrics.margin)
        assertTrue(metrics.fixedBrickHeights)
    }

    @Test
    fun expandedUsesFourColumnsWithDesktopMarginAndGutter() {
        val metrics = DashboardGridMetrics.forWidthClass(WindowWidthClass.Expanded, spacing)

        assertEquals(4, metrics.columns)
        assertEquals(20.dp, metrics.gutter)
        assertEquals(32.dp, metrics.margin)
        assertTrue(metrics.fixedBrickHeights)
    }

    @Test
    fun marginsAndGuttersGrowWithTheWidthClass() {
        val metrics = WindowWidthClass.entries.map { DashboardGridMetrics.forWidthClass(it, spacing) }

        assertEquals(metrics.sortedBy { it.margin.value }, metrics)
        assertEquals(metrics.sortedBy { it.gutter.value }, metrics)
        assertEquals(metrics.sortedBy { it.columns }, metrics)
    }

    @Test
    fun twoUnitBrickAlignsWithTwoStackedOneUnitBricksForEveryGutter() {
        val unit = 250.dp
        WindowWidthClass.entries
            .map { DashboardGridMetrics.forWidthClass(it, spacing) }
            .filter { it.fixedBrickHeights }
            .forEach { metrics ->
                val oneUnit = BrickHeight.OneUnit.resolve(unit, metrics.gutter)
                val twoUnits = BrickHeight.TwoUnits.resolve(unit, metrics.gutter)

                assertEquals(oneUnit + metrics.gutter + oneUnit, twoUnits)
            }
    }
}
