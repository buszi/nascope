package io.buszi.nascope.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.toWindowWidthClass
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen() {
    val viewModel = koinViewModel<DashboardViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DashboardScreen(
        viewState = viewState,
    )
}

@Composable
fun DashboardScreen(
    viewState: ViewState,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground),
    ) {
        val widthClass = maxWidth.toWindowWidthClass()
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
                val brickModifier = if (fixedHeights) {
                    Modifier.fillMaxWidth().height(
                        brick.height.resolve(
                            unit = AppTheme.dimensions.brickHeightUnit,
                            gutter = AppTheme.spacing.spaceMd,
                        ),
                    )
                } else {
                    Modifier.fillMaxWidth()
                }
                brick.Content(modifier = brickModifier)
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
