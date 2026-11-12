package io.buszi.nascope.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ConnectionCheckStatusUi
import io.buszi.nascope.ui.common.model.ConnectionCheckStepUi
import io.buszi.nascope.ui.common.model.ConnectionCheckUi
import io.buszi.nascope.ui.common.model.ConnectionFailureUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectionCheckRow(check: ConnectionCheckUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(TestTags.connectionCheck(check.step.name)),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.height(AppTheme.spacing.spaceLg),
            contentAlignment = Alignment.Center,
        ) {
            when (check.status) {
                ConnectionCheckStatusUi.Pending -> StatusDot(color = AppTheme.colors.textDisabled)
                ConnectionCheckStatusUi.Running -> StatusDot(color = AppTheme.colors.telemetryB, pulsing = true)
                ConnectionCheckStatusUi.Passed -> StatusDot(status = StatusLevel.Operational)
                ConnectionCheckStatusUi.Failed -> StatusDot(status = StatusLevel.Critical)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
            Text(
                text = stringResource(check.step.labelRes),
                style = AppTheme.typography.labelCode,
                color = if (check.status == ConnectionCheckStatusUi.Pending) AppTheme.colors.textMuted else AppTheme.colors.textPrimary,
                maxLines = 1,
            )
            if (check.failure != null) {
                Text(
                    text = stringResource(check.failure.messageRes),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.critical,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConnectionCheckRowPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            ConnectionCheckRow(ConnectionCheckUi(ConnectionCheckStepUi.ReachHost))
            ConnectionCheckRow(ConnectionCheckUi(ConnectionCheckStepUi.ReachHost, ConnectionCheckStatusUi.Running))
            ConnectionCheckRow(ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, ConnectionCheckStatusUi.Passed))
            ConnectionCheckRow(
                ConnectionCheckUi(
                    step = ConnectionCheckStepUi.Authenticate,
                    status = ConnectionCheckStatusUi.Failed,
                    failure = ConnectionFailureUi.Unauthorized,
                ),
            )
        }
    }
}
