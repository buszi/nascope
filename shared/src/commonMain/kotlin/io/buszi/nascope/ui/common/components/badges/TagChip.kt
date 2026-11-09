package io.buszi.nascope.ui.common.components.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Neutral metadata tag (4dp radius) for network interfaces, mount points,
 * container status codes, and other technical identifiers.
 *
 * Uses the sub-surface fill with a hairline border and secondary monospace text.
 *
 * @param text Custom label, e.g. `"eth0"`, `"/volume1"`, `"EXIT 137"`.
 * @param dotColor Optional leading [StatusDot] color; no dot is rendered when `null`.
 * @param pulsing Pulse animation hook forwarded to the [StatusDot] (ignored when [dotColor] is `null`).
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    pulsing: Boolean = false,
    backgroundColor: Color = AppTheme.colors.subSurface,
    borderColor: Color = AppTheme.colors.baseBorder,
    textColor: Color = AppTheme.colors.textSecondary,
) {
    ChipSurface(
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        modifier = modifier,
    ) {
        if (dotColor != null) {
            StatusDot(color = dotColor, pulsing = pulsing)
        }
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun TagChipPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                TagChip(text = "eth0")
                TagChip(text = "/volume1")
                TagChip(text = "EXIT 137")
            }
            TelemetryCard(title = "INTERFACES") {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    TagChip(text = "eth0", dotColor = AppTheme.colors.telemetryB, pulsing = true)
                    TagChip(text = "wlan0", dotColor = AppTheme.colors.textMuted)
                    TagChip(text = "10.0.4.88/24", backgroundColor = AppTheme.colors.inputBackground)
                }
            }
        }
    }
}
