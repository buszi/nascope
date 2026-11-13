package io.buszi.nascope.ui.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nodes_action_delete
import io.buszi.nascope.resources.nodes_action_edit
import io.buszi.nascope.resources.nodes_action_enter_password
import io.buszi.nascope.resources.nodes_password_missing
import io.buszi.nascope.resources.nodes_action_use
import io.buszi.nascope.resources.nodes_delete_cancel
import io.buszi.nascope.resources.nodes_delete_confirm
import io.buszi.nascope.resources.nodes_delete_prompt
import io.buszi.nascope.resources.nodes_marker_active
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction
import org.jetbrains.compose.resources.stringResource

@Composable
fun NodeCard(
    node: NodeUi,
    isConfirmingDelete: Boolean,
    onViewAction: (ViewAction) -> Unit,
    onEdit: () -> Unit,
) {
    TelemetryCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.nodesNode(node.id)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                val status = node.connectionStatus
                if (status != null) {
                    StatusDot(status = status.level, pulsing = status.pulsing)
                } else {
                    StatusDot(color = AppTheme.colors.textDisabled)
                }
                Text(
                    text = node.alias,
                    style = AppTheme.typography.headlineSm,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (node.isActive) {
                    TagChip(text = stringResource(Res.string.nodes_marker_active))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                TagChip(text = node.protocol.label)
                Text(
                    text = node.address,
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (node.isPasswordMissing) {
                Text(
                    text = stringResource(Res.string.nodes_password_missing),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.critical,
                    modifier = Modifier.testTag(TestTags.nodesPasswordMissing(node.id)),
                )
            }

            if (isConfirmingDelete) {
                Text(
                    text = stringResource(Res.string.nodes_delete_prompt, node.alias),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.critical,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    SecondaryButton(
                        text = stringResource(Res.string.nodes_delete_cancel),
                        onClick = { onViewAction(ViewAction.OnCancelDelete) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.NodesDeleteCancel),
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.nodes_delete_confirm),
                        onClick = { onViewAction(ViewAction.OnConfirmDelete) },
                        size = ButtonSize.Regular,
                        backgroundColor = AppTheme.colors.critical,
                        hoverColor = AppTheme.colors.critical.copy(alpha = HoverAlpha),
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.NodesDeleteConfirm),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    if (!node.isActive) {
                        PrimaryButton(
                            text = stringResource(Res.string.nodes_action_use),
                            onClick = { onViewAction(ViewAction.OnSetActive(node.id)) },
                            size = ButtonSize.Regular,
                            modifier = Modifier
                                .weight(1f)
                                .minimumTouchTarget()
                                .testTag(TestTags.nodesUse(node.id)),
                        )
                    }
                    val editModifier = Modifier
                        .weight(1f)
                        .minimumTouchTarget()
                        .testTag(TestTags.nodesEdit(node.id))
                    if (node.isPasswordMissing) {
                        // The fix is in the editor, which opens with the password field focused.
                        PrimaryButton(
                            text = stringResource(Res.string.nodes_action_enter_password),
                            onClick = onEdit,
                            size = ButtonSize.Regular,
                            modifier = editModifier,
                        )
                    } else {
                        SecondaryButton(
                            text = stringResource(Res.string.nodes_action_edit),
                            onClick = onEdit,
                            size = ButtonSize.Regular,
                            modifier = editModifier,
                        )
                    }
                    SecondaryButton(
                        text = stringResource(Res.string.nodes_action_delete),
                        onClick = { onViewAction(ViewAction.OnRequestDelete(node.id)) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.nodesDelete(node.id)),
                    )
                }
            }
        }
    }
}

private const val HoverAlpha = 0.85f
