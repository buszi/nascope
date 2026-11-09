package io.buszi.nascope.ui.common.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.badgeBackgroundColor
import io.buszi.nascope.ui.common.components.badges.badgeBorderColor
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.theme.AppTheme

private const val BackgroundAlpha: Float = 0.10f
private const val BorderAlpha: Float = 0.20f

/**
 * Visual badge displaying a telemetry rate-of-change or trend delta, e.g. `"+1.2% / 5m"`.
 *
 * Renders a compact 2dp rounded container (`shapes.sm`) with hairline border and a monospace
 * label with tabular numerals enabled (`labelCode`).
 *
 * @param text Formatted delta text, e.g. `"+1.2% / 5m"`, `"-1.8% / 5m"`, or `"+24 KB/s"`.
 * @param modifier Root layout modifier.
 * @param color Text color and base tint for border and background.
 * @param backgroundColor Optional background fill override (defaults to 10% opacity of [color]).
 * @param borderColor Optional border stroke override (defaults to 20% opacity of [color]).
 */
@Composable
fun TrendBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.operational,
    backgroundColor: Color = color.copy(alpha = BackgroundAlpha),
    borderColor: Color = color.copy(alpha = BorderAlpha),
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = AppTheme.shapes.sm,
            )
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = AppTheme.shapes.sm,
            )
            .padding(
                horizontal = AppTheme.spacing.spaceXs,
                vertical = AppTheme.spacing.space2xs,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * Overload of [TrendBadge] that derives colors directly from a semantic [StatusLevel].
 *
 * @param text Formatted delta text.
 * @param status Status level determining accent, background, and border tints.
 * @param modifier Root layout modifier.
 */
@Composable
fun TrendBadge(
    text: String,
    status: StatusLevel,
    modifier: Modifier = Modifier,
) {
    TrendBadge(
        text = text,
        modifier = modifier,
        color = status.color,
        backgroundColor = status.badgeBackgroundColor,
        borderColor = status.badgeBorderColor,
    )
}

@Preview
@Composable
private fun TrendBadgePreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                TrendBadge(text = "+1.2% / 5m", color = AppTheme.colors.operational)
                TrendBadge(text = "+12.4% / 5m", color = AppTheme.colors.telemetryA)
                TrendBadge(text = "-1.8% / 5m", color = AppTheme.colors.critical)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                TrendBadge(text = "STABLE", status = StatusLevel.Operational)
                TrendBadge(text = "+42ms JITTER", status = StatusLevel.Degraded)
                TrendBadge(text = "+98.4% PEAK", status = StatusLevel.Critical)
            }
        }
    }
}
