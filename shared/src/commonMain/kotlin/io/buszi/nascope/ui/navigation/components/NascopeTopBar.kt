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
import io.buszi.nascope.resources.nav_breadcrumb_connection_editor
import io.buszi.nascope.resources.nav_breadcrumb_dashboard
import io.buszi.nascope.resources.nav_breadcrumb_details
import io.buszi.nascope.resources.nav_breadcrumb_nodes
import io.buszi.nascope.resources.nav_breadcrumb_settings
import io.buszi.nascope.resources.nav_mode_demo
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.framework.title
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.navigation.NavRoute
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.Mode
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.ViewAction
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.ui.common.model.ConnectionStatusUi

@Composable
fun NascopeTopBar(
    currentRoute: NavRoute,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onManageNodes: () -> Unit,
) {
    val viewModel = koinViewModel<NascopeTopBarViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NascopeTopBarContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
        currentRoute = currentRoute,
        canNavigateBack = canNavigateBack,
        onNavigateBack = onNavigateBack,
        onManageNodes = onManageNodes,
    )
}

/**
 * Adaptive top control bar and app bar for the Nascope shell.
 *
 * Displays contextual back navigation when [canNavigateBack] is `true`, the active route
 * breadcrumb path, and the data source: the connected node with its live status, or a `DEMO` chip.
 */
@Composable
fun NascopeTopBarContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    currentRoute: NavRoute,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onManageNodes: () -> Unit,
) {
    val windowWidthClass = AppTheme.windowWidthClass
    val breadcrumbText = when (currentRoute) {
        NavRoute.Dashboard -> stringResource(Res.string.nav_breadcrumb_dashboard)
        is NavRoute.Details -> stringResource(
            Res.string.nav_breadcrumb_details,
            currentRoute.brickId.title(compact = windowWidthClass == WindowWidthClass.Compact),
        )
        NavRoute.Nodes -> stringResource(Res.string.nav_breadcrumb_nodes)
        is NavRoute.ConnectionEditor -> stringResource(Res.string.nav_breadcrumb_connection_editor)
        NavRoute.Settings -> stringResource(Res.string.nav_breadcrumb_settings)
    }

    Column(
        modifier = Modifier
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
                when (viewState.mode) {
                    Mode.None -> Unit
                    Mode.Demo -> TagChip(
                        text = stringResource(Res.string.nav_mode_demo),
                        modifier = Modifier.testTag(TestTags.TopBarDemo),
                    )

                    Mode.Server -> {
                        if (windowWidthClass != WindowWidthClass.Compact) {
                            NodeSwitcher(
                                activeNode = viewState.nodes.firstOrNull { it.isActive },
                                nodeName = viewState.nodeName,
                                nodes = viewState.nodes,
                                onSelectNode = { onViewAction(ViewAction.OnSelectNode(it)) },
                                onManageNodes = onManageNodes,
                            )
                        }
                        StatusChip(
                            text = stringResource(viewState.connectionStatus.labelRes),
                            status = viewState.connectionStatus.level,
                            pulsing = viewState.connectionStatus.pulsing,
                            modifier = Modifier.testTag(TestTags.TopBarStatus),
                        )
                    }
                }
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
            NascopeTopBarContent(
                onViewAction = {},
                viewState = ViewState(
                    mode = Mode.Server,
                    nodeName = "alpha-node-01",
                    connectionStatus = ConnectionStatusUi.Online,
                ),
                currentRoute = NavRoute.Dashboard,
                canNavigateBack = false,
                onNavigateBack = {},
                onManageNodes = {},
            )
            NascopeTopBarContent(
                onViewAction = {},
                viewState = ViewState(
                    mode = Mode.Server,
                    nodeName = "alpha-node-01",
                    connectionStatus = ConnectionStatusUi.Offline,
                ),
                currentRoute = NavRoute.Details(DashboardBrickId.CpuMatrix),
                canNavigateBack = true,
                onNavigateBack = {},
                onManageNodes = {},
            )
            NascopeTopBarContent(
                onViewAction = {},
                viewState = ViewState(
                    mode = Mode.Server,
                    nodeName = "alpha-node-01",
                    connectionStatus = ConnectionStatusUi.Connecting,
                ),
                currentRoute = NavRoute.Nodes,
                canNavigateBack = false,
                onNavigateBack = {},
                onManageNodes = {},
            )
            NascopeTopBarContent(
                onViewAction = {},
                viewState = ViewState(mode = Mode.Demo),
                currentRoute = NavRoute.Settings,
                canNavigateBack = false,
                onNavigateBack = {},
                onManageNodes = {},
            )
        }
    }
}
