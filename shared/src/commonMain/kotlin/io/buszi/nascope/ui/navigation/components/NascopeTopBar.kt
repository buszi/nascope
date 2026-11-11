package io.buszi.nascope.ui.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nav_action_back
import io.buszi.nascope.resources.nav_active_node_name
import io.buszi.nascope.resources.nav_breadcrumb_dashboard
import io.buszi.nascope.resources.nav_breadcrumb_details
import io.buszi.nascope.resources.nav_breadcrumb_nodes
import io.buszi.nascope.resources.nav_breadcrumb_settings
import io.buszi.nascope.resources.nav_status_online
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.navigation.NavRoute
import org.jetbrains.compose.resources.stringResource

/**
 * Adaptive top control bar and app bar for the Nascope shell.
 *
 * Displays contextual back navigation when [canNavigateBack] is `true`, the active route
 * breadcrumb path, and live node telemetry health indicators.
 *
 * @param currentRoute Currently active [NavRoute] rendered in the main workspace.
 * @param canNavigateBack Whether the back button should be visible and enabled.
 * @param onNavigateBack Callback invoked when the back control is activated.
 * @param modifier Root layout modifier applied to the top bar container.
 */
@Composable
fun NascopeTopBar(
    currentRoute: NavRoute,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowWidthClass = AppTheme.windowWidthClass
    val breadcrumbText = when (currentRoute) {
        NavRoute.Dashboard -> stringResource(Res.string.nav_breadcrumb_dashboard)
        is NavRoute.Details -> stringResource(Res.string.nav_breadcrumb_details, currentRoute.brickId.name)
        NavRoute.Nodes -> stringResource(Res.string.nav_breadcrumb_nodes)
        NavRoute.Settings -> stringResource(Res.string.nav_breadcrumb_settings)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.subSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.topBarHeight)
                .padding(horizontal = AppTheme.spacing.spaceLg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            ) {
                if (canNavigateBack) {
                    TopBarBackButton(
                        showLabel = windowWidthClass != WindowWidthClass.Compact,
                        onClick = onNavigateBack,
                    )
                }

                Text(
                    text = breadcrumbText,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                if (windowWidthClass != WindowWidthClass.Compact) {
                    TagChip(
                        text = stringResource(Res.string.nav_active_node_name),
                        dotColor = AppTheme.colors.operational,
                        pulsing = true,
                    )
                }

                StatusChip(
                    text = stringResource(Res.string.nav_status_online),
                    status = StatusLevel.Operational,
                    pulsing = true,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.baseBorder),
        )
    }
}

@Composable
private fun TopBarBackButton(
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = AppTheme.colors.elevatedBorder,
        label = "TopBarBackBorder",
    )

    Row(
        modifier = Modifier
            .height(AppTheme.dimensions.buttonHeightDense)
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.elevatedOverlay, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = AppTheme.shapes.base,
            )
            .testTag(TestTags.TopBarBack)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        NavigationGlyph(
            type = NavigationGlyphType.Back,
            tint = AppTheme.colors.textPrimary,
            iconSize = AppTheme.spacing.spaceLg,
        )
        if (showLabel) {
            Text(
                text = stringResource(Res.string.nav_action_back),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}

@Preview
@Composable
private fun NascopeTopBarPreview() {
    AppTheme {
        Column(
            modifier = Modifier.background(AppTheme.colors.canvasBackground),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            NascopeTopBar(
                currentRoute = NavRoute.Dashboard,
                canNavigateBack = false,
                onNavigateBack = {},
            )
            NascopeTopBar(
                currentRoute = NavRoute.Details(DashboardBrickId.CpuMatrix),
                canNavigateBack = true,
                onNavigateBack = {},
            )
        }
    }
}
