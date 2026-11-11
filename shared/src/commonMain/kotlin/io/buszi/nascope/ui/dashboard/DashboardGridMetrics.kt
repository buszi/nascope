package io.buszi.nascope.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppSpacing
import io.buszi.nascope.ui.common.util.WindowWidthClass

/**
 * Layout metrics of the dashboard grid for one width class.
 *
 * | Class | Columns | Gutter | Margin | Brick heights |
 * |---|---|---|---|---|
 * | [WindowWidthClass.Compact] | 2 | 12dp | 16dp | natural (wrap content) |
 * | [WindowWidthClass.Medium] | 3 | 16dp | 24dp | modular 1U steps |
 * | [WindowWidthClass.Expanded] | 4 | 20dp | 32dp | modular 1U steps |
 *
 * @param columns Number of equal grid columns.
 * @param gutter Gap between neighbouring bricks, horizontally and vertically. Also the gap that
 * [io.buszi.nascope.ui.dashboard.framework.BrickHeight] adds between stacked units.
 * @param margin Padding around the whole grid.
 * @param fixedBrickHeights Whether bricks get the modular height of their [io.buszi.nascope.ui.dashboard.framework.BrickHeight].
 */
@Immutable
data class DashboardGridMetrics(
    val columns: Int,
    val gutter: Dp,
    val margin: Dp,
    val fixedBrickHeights: Boolean,
) {
    companion object {
        fun forWidthClass(widthClass: WindowWidthClass, spacing: AppSpacing): DashboardGridMetrics =
            when (widthClass) {
                WindowWidthClass.Compact -> DashboardGridMetrics(
                    columns = 2,
                    gutter = spacing.spaceMd,
                    margin = spacing.margin,
                    fixedBrickHeights = false,
                )
                WindowWidthClass.Medium -> DashboardGridMetrics(
                    columns = 3,
                    gutter = spacing.gutter,
                    margin = spacing.marginTablet,
                    fixedBrickHeights = true,
                )
                WindowWidthClass.Expanded -> DashboardGridMetrics(
                    columns = 4,
                    gutter = spacing.gutterDesktop,
                    margin = spacing.marginDesktop,
                    fixedBrickHeights = true,
                )
            }
    }
}
