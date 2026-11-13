package io.buszi.nascope.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalContentInsets
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.toWindowWidthClass
import io.buszi.nascope.ui.dashboard.framework.BrickInteraction
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.LocalBrickInteraction
import io.buszi.nascope.ui.dashboard.framework.computeBrickSpans

/**
 * Responsive grid of dashboard [bricks].
 *
 * The column count, gutter, margin and brick height policy follow the width class of the space the grid
 * is laid out in ([DashboardGridMetrics]); [computeBrickSpans] turns the bricks' [DashboardBrick.span]s into cell
 * counts so that no row has a hole, whichever bricks are visible.
 * The class is derived from the grid's own width plus the navigation rail, so opening the details
 * inspector next to the dashboard reflows the grid to the narrower layout instead of squeezing it.
 *
 * @param bricks Bricks to lay out, in order.
 * @param modifier Layout modifier of the grid container.
 * @param selectedBrickId Brick whose details are currently open, rendered with the open affordance.
 * @param onOpenDetails Invoked when a brick that has details is tapped.
 * @param header Optional full-width item placed above the bricks.
 */
@Composable
fun AdaptiveDashboardGrid(
    bricks: List<DashboardBrick>,
    modifier: Modifier = Modifier,
    selectedBrickId: DashboardBrickId? = null,
    onOpenDetails: (DashboardBrickId) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val effectiveWidth = if (AppTheme.windowWidthClass != WindowWidthClass.Compact) {
            maxWidth + AppTheme.dimensions.sidebarTabletWidth
        } else {
            maxWidth
        }
        val metrics = DashboardGridMetrics.forWidthClass(
            widthClass = effectiveWidth.toWindowWidthClass(),
            spacing = AppTheme.spacing,
        )

        val spans = remember(bricks, metrics.columns) {
            computeBrickSpans(bricks.map { it.span }, metrics.columns)
        }

        // Bricks scroll behind the system bars; the margin is added on top of what the bars cover.
        val insets = LocalContentInsets.current.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues.Absolute(
            left = metrics.margin + insets.calculateLeftPadding(layoutDirection),
            top = metrics.margin + insets.calculateTopPadding(),
            right = metrics.margin + insets.calculateRightPadding(layoutDirection),
            bottom = metrics.margin + insets.calculateBottomPadding(),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.DashboardGrid),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(metrics.gutter),
            verticalArrangement = Arrangement.spacedBy(metrics.gutter),
        ) {
            if (header != null) {
                item(
                    key = "dashboard_header",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    header()
                }
            }

            itemsIndexed(
                items = bricks,
                key = { _, brick -> brick.id.name },
                span = { index, _ -> GridItemSpan(spans.getOrElse(index) { 1 }) },
            ) { _, brick ->
                val baseModifier = Modifier
                    .fillMaxWidth()
                    .optionalModifier(metrics.fixedBrickHeights) {
                        height(
                            brick.height.resolve(
                                unit = AppTheme.dimensions.brickHeightUnit,
                                gutter = metrics.gutter,
                            ),
                        )
                    }
                val hasDetails = brick.details != null
                val interaction = when {
                    !hasDetails -> BrickInteraction.None
                    brick.id == selectedBrickId -> BrickInteraction.Open
                    else -> BrickInteraction.Openable
                }
                CompositionLocalProvider(LocalBrickInteraction provides interaction) {
                    brick.Content(
                        modifier = baseModifier
                            .clip(AppTheme.shapes.lg)
                            .testTag(TestTags.brick(brick.id))
                            .optionalModifier(hasDetails) {
                                semantics { selected = interaction == BrickInteraction.Open }
                                    .clickable { onOpenDetails(brick.id) }
                            },
                    )
                }
            }
        }
    }
}
