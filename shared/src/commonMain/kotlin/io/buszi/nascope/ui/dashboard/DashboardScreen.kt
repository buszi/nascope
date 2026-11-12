package io.buszi.nascope.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.dashboard_header_demo_node
import io.buszi.nascope.resources.dashboard_header_alerts
import io.buszi.nascope.resources.dashboard_header_label
import io.buszi.nascope.resources.dashboard_refresh
import io.buszi.nascope.resources.nav_mode_demo
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.AlertBadgeUi
import io.buszi.nascope.ui.common.model.SessionStatusUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun DashboardScreen(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    selectedBrickId: DashboardBrickId? = null,
    onOpenDetails: (DashboardBrickId) -> Unit = {},
    onViewAction: (ViewAction) -> Unit = {},
) {
    AdaptiveDashboardGrid(
        bricks = viewState.bricks,
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground),
        selectedBrickId = selectedBrickId,
        onOpenDetails = onOpenDetails,
        header = {
            DashboardHeader(
                nodeName = if (viewState.sessionStatus == SessionStatusUi.Demo) {
                    stringResource(Res.string.dashboard_header_demo_node)
                } else {
                    viewState.nodeName
                },
                status = viewState.sessionStatus,
                alertBadge = viewState.alertBadge,
                onRefresh = { onViewAction(ViewAction.OnRefresh) },
            )
        },
    )
}

@Composable
private fun DashboardHeader(
    nodeName: String,
    status: SessionStatusUi,
    alertBadge: AlertBadgeUi?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.dashboard_header_label),
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (alertBadge != null) {
                StatusChip(
                    text = pluralStringResource(Res.plurals.dashboard_header_alerts, alertBadge.count, alertBadge.count),
                    status = alertBadge.status,
                    pulsing = alertBadge.status == StatusLevel.Critical,
                    modifier = Modifier.testTag(TestTags.DashboardAlerts),
                )
            }
            // On Compact the top bar is right above and already shows the status.
            val visibleStatus = if (AppTheme.windowWidthClass == WindowWidthClass.Compact) SessionStatusUi.None else status
            when (visibleStatus) {
                SessionStatusUi.None -> Unit
                SessionStatusUi.Demo -> TagChip(text = stringResource(Res.string.nav_mode_demo))
                is SessionStatusUi.Server -> StatusChip(
                    text = stringResource(visibleStatus.connection.labelRes),
                    status = visibleStatus.connection.level,
                    pulsing = visibleStatus.connection.pulsing,
                    modifier = Modifier.testTag(TestTags.DashboardStatus),
                )
            }
            SecondaryButton(
                text = stringResource(Res.string.dashboard_refresh),
                onClick = onRefresh,
                modifier = Modifier.testTag(TestTags.DashboardRefresh),
            )
        }
    }
}

@Composable
@Preview
private fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            viewState = ViewState(
                bricks = emptyList(),
                nodeName = "nas-01",
                sessionStatus = SessionStatusUi.Server(ConnectionStatusUi.Online),
            ),
        )
    }
}
