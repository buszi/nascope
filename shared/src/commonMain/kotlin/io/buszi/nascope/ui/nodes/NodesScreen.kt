package io.buszi.nascope.ui.nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nodes_action_add
import io.buszi.nascope.resources.nodes_demo_connect
import io.buszi.nascope.resources.nodes_demo_description
import io.buszi.nascope.resources.nodes_demo_exit
import io.buszi.nascope.resources.nodes_demo_title
import io.buszi.nascope.resources.nodes_demo_use
import io.buszi.nascope.resources.nodes_empty
import io.buszi.nascope.resources.nodes_title
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.CenteredPanel
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.common.model.ServerProtocolUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NodesScreen(
    onAddServer: () -> Unit,
    onEditServer: (id: String) -> Unit,
) {
    val viewModel = koinViewModel<NodesViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NodesContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
        onAddServer = onAddServer,
        onEditServer = onEditServer,
    )
}

@Composable
fun NodesContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (id: String) -> Unit,
) {
    CenteredPanel(
        modifier = Modifier
            .background(AppTheme.colors.canvasBackground)
            .testTag(TestTags.NodesScreen),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.nodes_title),
                style = AppTheme.typography.headlineLg,
                color = AppTheme.colors.textPrimary,
            )
            PrimaryButton(
                text = stringResource(Res.string.nodes_action_add),
                onClick = onAddServer,
                size = ButtonSize.Regular,
                modifier = Modifier
                    .minimumTouchTarget()
                    .testTag(TestTags.NodesAdd),
            )
        }

        if (viewState.isDemo) {
            DemoNotice(onViewAction = onViewAction, onAddServer = onAddServer)
        }

        if (viewState.nodes.isEmpty() && !viewState.isLoading) {
            Text(
                text = stringResource(Res.string.nodes_empty),
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textSecondary,
            )
        }

        viewState.nodes.forEach { node ->
            NodeCard(
                node = node,
                isConfirmingDelete = node.id == viewState.pendingDeleteId,
                onViewAction = onViewAction,
                onEdit = { onEditServer(node.id) },
            )
        }

        if (!viewState.isDemo && !viewState.isLoading) {
            SecondaryButton(
                text = stringResource(Res.string.nodes_demo_use),
                onClick = { onViewAction(ViewAction.OnUseDemo) },
                size = ButtonSize.Regular,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.NodesUseDemo),
            )
        }
    }
}

@Composable
private fun DemoNotice(
    onViewAction: (ViewAction) -> Unit,
    onAddServer: () -> Unit,
) {
    TelemetryCard(
        title = stringResource(Res.string.nodes_demo_title),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NodesDemoNotice),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            Text(
                text = stringResource(Res.string.nodes_demo_description),
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                PrimaryButton(
                    text = stringResource(Res.string.nodes_demo_connect),
                    onClick = onAddServer,
                    size = ButtonSize.Regular,
                    modifier = Modifier
                        .weight(1f)
                        .minimumTouchTarget()
                        .testTag(TestTags.NodesDemoConnect),
                )
                SecondaryButton(
                    text = stringResource(Res.string.nodes_demo_exit),
                    onClick = { onViewAction(ViewAction.OnExitDemo) },
                    size = ButtonSize.Regular,
                    modifier = Modifier
                        .weight(1f)
                        .minimumTouchTarget()
                        .testTag(TestTags.NodesDemoExit),
                )
            }
        }
    }
}

@Preview
@Composable
private fun NodesContentPreview() {
    AppTheme {
        NodesContent(
            viewState = ViewState(
                nodes = listOf(
                    NodeUi(
                        id = "alpha",
                        alias = "alpha-node-01",
                        host = "alpha-node-01.local",
                        port = 61208,
                        protocol = ServerProtocolUi.Http,
                        isActive = true,
                        connectionStatus = ConnectionStatusUi.Online,
                    ),
                    NodeUi(
                        id = "beta",
                        alias = "backup-nas",
                        host = "10.0.0.2",
                        port = 8443,
                        protocol = ServerProtocolUi.Https,
                    ),
                ),
                isLoading = false,
            ),
            onViewAction = {},
            onAddServer = {},
            onEditServer = {},
        )
    }
}

@Preview
@Composable
private fun NodesContentPasswordMissingPreview() {
    AppTheme {
        NodesContent(
            viewState = ViewState(
                nodes = listOf(
                    NodeUi(
                        id = "alpha",
                        alias = "alpha-node-01",
                        host = "alpha-node-01.local",
                        port = 61208,
                        protocol = ServerProtocolUi.Http,
                        isActive = true,
                        connectionStatus = ConnectionStatusUi.PasswordMissing,
                    ),
                ),
                isLoading = false,
            ),
            onViewAction = {},
            onAddServer = {},
            onEditServer = {},
        )
    }
}

@Preview
@Composable
private fun NodesContentDemoPreview() {
    AppTheme {
        NodesContent(
            viewState = ViewState(
                nodes = listOf(
                    NodeUi(
                        id = "beta",
                        alias = "backup-nas",
                        host = "10.0.0.2",
                        port = 8443,
                        protocol = ServerProtocolUi.Https,
                    ),
                ),
                isDemo = true,
                isLoading = false,
            ),
            onViewAction = {},
            onAddServer = {},
            onEditServer = {},
        )
    }
}

@Preview
@Composable
private fun NodesContentDeletePreview() {
    AppTheme {
        NodesContent(
            viewState = ViewState(
                nodes = listOf(
                    NodeUi(
                        id = "beta",
                        alias = "backup-nas",
                        host = "10.0.0.2",
                        port = 8443,
                        protocol = ServerProtocolUi.Https,
                    ),
                ),
                pendingDeleteId = "beta",
                isLoading = false,
            ),
            onViewAction = {},
            onAddServer = {},
            onEditServer = {},
        )
    }
}
