package io.buszi.nascope.ui.common.components.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Layer 2 Surface Container (Elevated Overlay).
 * Used for floating popovers, command menus, dropdowns, and modal dialogs.
 * Uses Elevated Surface fill (#181C24) with elevated border (#282E3E).
 */
@Composable
fun ElevatedPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.lg,
    backgroundColor: Color = AppTheme.colors.elevatedOverlay,
    borderColor: Color = AppTheme.colors.elevatedBorder,
    borderWidth: Dp = AppTheme.dimensions.hairlineBorder,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.spaceLg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Preview
@Composable
private fun ElevatedPanelPreview() {
    AppTheme {
        ElevatedPanel {
            Text(
                text = "COMMAND OVERLAY",
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                text = "Elevated Surface Layer 2",
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}
