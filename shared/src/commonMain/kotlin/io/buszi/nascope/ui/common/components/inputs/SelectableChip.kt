package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Compact toggle chip for single-choice option groups such as sort orders and status filters.
 *
 * @param text Technical label rendered in code typography, e.g. `"USAGE"`.
 * @param selected Whether this option is the active one.
 * @param onClick Invoked when the chip is tapped.
 * @param modifier Root layout modifier.
 */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.base
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                color = if (selected) AppTheme.colors.telemetryA.copy(alpha = 0.14f) else AppTheme.colors.inputBackground,
                shape = shape,
            )
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (selected) AppTheme.colors.telemetryA else AppTheme.colors.baseBorder,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.spaceSm, vertical = AppTheme.spacing.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = if (selected) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SelectableChipPreview() {
    AppTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
            SelectableChip(text = "USAGE", selected = true, onClick = {})
            SelectableChip(text = "NAME", selected = false, onClick = {})
        }
    }
}
