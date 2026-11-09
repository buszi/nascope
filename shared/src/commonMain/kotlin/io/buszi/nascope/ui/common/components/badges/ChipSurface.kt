package io.buszi.nascope.ui.common.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Shared chip substrate for [StatusChip] and [TagChip]:
 * 4dp radius, 1px hairline border, dense horizontal row.
 *
 * Intentionally not clipped, so status dot halos / pulse ripples may bleed past the content box.
 */
@Composable
internal fun ChipSurface(
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.base,
    borderWidth: Dp = AppTheme.dimensions.hairlineBorder,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = AppTheme.spacing.spaceSm,
        vertical = AppTheme.spacing.space2xs,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs + AppTheme.spacing.space2xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
