package io.buszi.nascope.ui.common.components.badges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Semantic status badge (4dp radius) for Operational / Degraded / Critical states.
 *
 * Renders a tinted fill + hairline border, an optional 6dp [StatusDot] with luminous halo,
 * and a monospace label. Colors cross-fade when [status] changes.
 *
 * @param text Custom label, e.g. `"ONLINE"`, `"SYNC: LATENCY"`.
 * @param showDot Whether to render the leading status dot.
 * @param pulsing Pulse animation hook forwarded to the [StatusDot] (ignored when [showDot] is `false`).
 */
@Composable
fun StatusChip(
    text: String,
    status: StatusLevel,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
    pulsing: Boolean = false,
) {
    val accentColor by animateColorAsState(status.color, label = "StatusChipAccent")
    val haloColor by animateColorAsState(status.haloColor, label = "StatusChipHalo")
    val backgroundColor by animateColorAsState(status.badgeBackgroundColor, label = "StatusChipBackground")
    val borderColor by animateColorAsState(status.badgeBorderColor, label = "StatusChipBorder")

    ChipSurface(
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        modifier = modifier,
    ) {
        if (showDot) {
            StatusDot(
                color = accentColor,
                haloColor = haloColor,
                pulsing = pulsing,
            )
        }
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = accentColor,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun StatusChipPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                StatusChip(text = "OPERATIONAL", status = StatusLevel.Operational)
                StatusChip(text = "DEGRADED", status = StatusLevel.Degraded)
                StatusChip(text = "CRITICAL", status = StatusLevel.Critical, pulsing = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                StatusChip(text = "KUBE: HEALTHY", status = StatusLevel.Operational, showDot = false)
                StatusChip(text = "SYNC: LATENCY", status = StatusLevel.Degraded, showDot = false)
                StatusChip(text = "RAID: FAILED", status = StatusLevel.Critical, showDot = false)
            }
        }
    }
}
