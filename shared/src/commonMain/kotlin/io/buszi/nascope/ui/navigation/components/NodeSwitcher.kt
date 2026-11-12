package io.buszi.nascope.ui.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nav_node_switcher_manage
import io.buszi.nascope.resources.nodes_marker_active
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.components.containers.ElevatedPanel
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.testing.testTagsAsResourceId
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The node chip of the top bar. Tapping it opens a menu of the stored servers to switch between,
 * plus a shortcut to the node management screen.
 */
@Composable
fun NodeSwitcher(
    activeNode: NodeUi?,
    nodeName: String,
    nodes: List<NodeUi>,
    onSelectNode: (id: String) -> Unit,
    onManageNodes: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val status = activeNode?.connectionStatus

    Box {
        TagChip(
            text = nodeName,
            dotColor = status?.level?.color,
            pulsing = status?.pulsing ?: false,
            modifier = Modifier
                .clip(AppTheme.shapes.base)
                .clickable(role = Role.Button) { expanded = true }
                .testTag(TestTags.TopBarNode),
        )
        if (expanded) {
            NodeSwitcherMenu(
                nodes = nodes,
                onSelectNode = {
                    expanded = false
                    onSelectNode(it)
                },
                onManageNodes = {
                    expanded = false
                    onManageNodes()
                },
                onDismiss = { expanded = false },
            )
        }
    }
}

@Composable
private fun NodeSwitcherMenu(
    nodes: List<NodeUi>,
    onSelectNode: (id: String) -> Unit,
    onManageNodes: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gap = with(LocalDensity.current) { AppTheme.spacing.spaceXs.roundToPx() }
    val positionProvider = remember(gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset(
                x = (anchorBounds.right - popupContentSize.width).coerceAtLeast(0),
                y = anchorBounds.bottom + gap,
            )
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        ElevatedPanel(
            modifier = Modifier
                .testTagsAsResourceId()
                .width(AppTheme.dimensions.nodeSwitcherWidth),
            contentPadding = PaddingValues(AppTheme.spacing.spaceXs),
        ) {
            nodes.forEach { node ->
                NodeOption(node = node, onClick = { onSelectNode(node.id) })
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.spaceXs)
                    .height(AppTheme.dimensions.hairlineBorder)
                    .background(AppTheme.colors.subtleDivider),
            )
            Text(
                text = stringResource(Res.string.nav_node_switcher_manage),
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.base)
                    .clickable(role = Role.Button, onClick = onManageNodes)
                    .padding(AppTheme.spacing.spaceSm)
                    .testTag(TestTags.TopBarManageNodes),
            )
        }
    }
}

@Composable
private fun NodeOption(
    node: NodeUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AppTheme.spacing.spaceSm)
            .testTag(TestTags.topBarNodeOption(node.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        val status = node.connectionStatus
        if (status != null) {
            StatusDot(status = status.level, pulsing = status.pulsing)
        } else {
            StatusDot(color = AppTheme.colors.textDisabled)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.alias,
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = node.address,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (node.isActive) {
            TagChip(text = stringResource(Res.string.nodes_marker_active))
        }
    }
}
