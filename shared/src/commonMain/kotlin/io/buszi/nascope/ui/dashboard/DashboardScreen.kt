package io.buszi.nascope.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.toWindowWidthClass
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.BrickInteraction
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.LocalBrickInteraction
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    selectedBrickId: DashboardBrickId? = null,
    onOpenDetails: (DashboardBrickId) -> Unit = {},
) {
    val viewModel = koinViewModel<DashboardViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DashboardScreen(
        viewState = viewState,
        modifier = modifier,
        selectedBrickId = selectedBrickId,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
fun DashboardScreen(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    selectedBrickId: DashboardBrickId? = null,
    onOpenDetails: (DashboardBrickId) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground),
    ) {
        val effectiveWidth = if (AppTheme.windowWidthClass != WindowWidthClass.Compact) {
            maxWidth + AppTheme.dimensions.sidebarTabletWidth
        } else {
            maxWidth
        }
        val widthClass = effectiveWidth.toWindowWidthClass()
        val fixedHeights = widthClass != WindowWidthClass.Compact
        val columnCount = when (widthClass) {
            WindowWidthClass.Compact -> 2
            WindowWidthClass.Medium -> 3
            WindowWidthClass.Expanded -> 4
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spacing.margin),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            item(
                key = "dashboard_header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                DashboardHeader(
                    systemLabel = "SYSTEM MONITOR // OBSIDIAN TELEMETRY",
                    nodeName = "Node Cluster alpha-01",
                    status = StatusLevel.Operational,
                    statusText = "ONLINE",
                )
            }

            items(
                items = viewState.bricks,
                key = { it.id.name },
                span = { brick -> brick.span.calculateSpan(this) },
            ) { brick ->
                val baseModifier = Modifier
                    .fillMaxWidth()
                    .optionalModifier(fixedHeights) {
                        height(
                            brick.height.resolve(
                                unit = AppTheme.dimensions.brickHeightUnit,
                                gutter = AppTheme.spacing.spaceMd,
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
                            .optionalModifier(hasDetails) {
                                clickable { onOpenDetails(brick.id) }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    systemLabel: String,
    nodeName: String,
    status: StatusLevel,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = systemLabel,
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
            Text(
                text = nodeName,
                style = AppTheme.typography.headlineLg,
                color = AppTheme.colors.textPrimary,
            )
        }

        StatusChip(
            text = statusText,
            status = status,
            pulsing = true,
        )
    }
}

@Composable
@Preview
private fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            viewState = ViewState(
                bricks = emptyList(),
            ),
        )
    }
}
