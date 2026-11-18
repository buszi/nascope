package io.buszi.nascope.ui.dashboard.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.customize_bricks_helper
import io.buszi.nascope.resources.customize_bricks_section
import io.buszi.nascope.resources.customize_not_offered
import io.buszi.nascope.resources.customize_reset
import io.buszi.nascope.resources.customize_reset_cancel
import io.buszi.nascope.resources.customize_reset_confirm
import io.buszi.nascope.resources.customize_reset_helper
import io.buszi.nascope.resources.customize_reset_prompt
import io.buszi.nascope.resources.customize_reset_section
import io.buszi.nascope.resources.customize_subtitle
import io.buszi.nascope.resources.customize_title
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.inputs.TelemetrySwitch
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.title
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardCustomizeScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<DashboardCustomizeViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DashboardCustomizeScreen(
        viewState = viewState,
        onClose = onClose,
        onViewAction = viewModel::onViewAction,
        modifier = modifier,
    )
}

/**
 * The dashboard editor: a switch per registered brick in the layout's order, and "Reset to default". It sits in the
 * inspector pane beside the live dashboard (Medium / Expanded) or fills the screen (Compact).
 */
@Composable
fun DashboardCustomizeScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customize = viewState.customize

    DetailsScaffold(
        title = stringResource(Res.string.customize_title),
        onClose = onClose,
        testTag = TestTags.CustomizeScreen,
        modifier = modifier,
        subtitle = customize?.let { stringResource(Res.string.customize_subtitle, it.enabledCount, it.rows.size) },
    ) {
        if (customize == null) {
            DetailsLoadingPlaceholder()
            return@DetailsScaffold
        }

        DetailsSection(title = stringResource(Res.string.customize_bricks_section)) {
            Text(
                text = stringResource(Res.string.customize_bricks_helper),
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textSecondary,
            )
            Column(modifier = Modifier.padding(top = AppTheme.spacing.spaceSm)) {
                customize.rows.forEach { row ->
                    BrickToggleRow(
                        row = row,
                        onToggle = { enabled -> onViewAction(ViewAction.OnToggleBrick(row.id, enabled)) },
                    )
                }
            }
        }

        ResetSection(
            isCustomized = customize.isCustomized,
            isConfirming = viewState.isConfirmingReset,
            onViewAction = onViewAction,
        )
    }
}

@Composable
private fun BrickToggleRow(
    row: DashboardCustomizeUi.BrickRow,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = row.id.title()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.customizeRow(row.key))
            .padding(vertical = AppTheme.spacing.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            // A brick that is off keeps its place, dimmed, so turning it back on restores where it was.
            Text(
                text = title,
                style = AppTheme.typography.labelUi,
                color = if (row.isEnabled) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!row.isOffered) {
                Text(
                    text = stringResource(Res.string.customize_not_offered),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TelemetrySwitch(
            checked = row.isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier
                .semantics { contentDescription = title }
                .testTag(TestTags.customizeSwitch(row.key)),
        )
    }
}

@Composable
private fun ResetSection(
    isCustomized: Boolean,
    isConfirming: Boolean,
    onViewAction: (ViewAction) -> Unit,
) {
    DetailsSection(title = stringResource(Res.string.customize_reset_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            if (isConfirming) {
                Text(
                    text = stringResource(Res.string.customize_reset_prompt),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    SecondaryButton(
                        text = stringResource(Res.string.customize_reset_cancel),
                        onClick = { onViewAction(ViewAction.OnCancelReset) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.CustomizeResetCancel),
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.customize_reset_confirm),
                        onClick = { onViewAction(ViewAction.OnConfirmReset) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.CustomizeResetConfirm),
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.customize_reset_helper),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textSecondary,
                )
                SecondaryButton(
                    text = stringResource(Res.string.customize_reset),
                    onClick = { onViewAction(ViewAction.OnRequestReset) },
                    size = ButtonSize.Regular,
                    enabled = isCustomized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .testTag(TestTags.CustomizeReset),
                )
            }
        }
    }
}

@Preview
@Composable
private fun DashboardCustomizeScreenPreview(
    @PreviewParameter(DashboardCustomizePreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        DashboardCustomizeScreen(viewState = viewState, onClose = {}, onViewAction = {})
    }
}
