package io.buszi.nascope.ui.common.components.badges

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

private const val HaloScale = 5f / 3f // 6dp dot -> 10dp halo
private const val PulseMaxScale = 2.5f // 6dp dot -> 15dp ripple
private const val PulseMaxAlpha = 0.45f
private const val PulseDurationMillis = 1400

/**
 * Status indicator dot (6dp by default) with a static luminous halo.
 *
 * The halo and pulse ripple are drawn behind the dot without affecting layout size,
 * so the dot can be aligned with text baselines like a regular 6dp element.
 *
 * @param pulsing Pulse animation hook. When `true`, an expanding, fading ripple is emitted
 * from the dot on an infinite loop (e.g. live/critical states).
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    haloColor: Color = color.copy(alpha = 0.12f),
    pulsing: Boolean = false,
    size: Dp = AppTheme.dimensions.statusDotSize,
) {
    val pulseProgress: State<Float>? = if (pulsing) {
        rememberInfiniteTransition(label = "StatusDotPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(PulseDurationMillis, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "StatusDotPulseProgress",
        )
    } else {
        null
    }

    Spacer(
        modifier = modifier
            .size(size)
            .drawBehind {
                val radius = this.size.minDimension / 2f
                drawCircle(color = haloColor, radius = radius * HaloScale)
                // State is read in the draw phase only, so pulsing never triggers recomposition.
                pulseProgress?.value?.let { progress ->
                    drawCircle(
                        color = color.copy(alpha = color.alpha * PulseMaxAlpha * (1f - progress)),
                        radius = radius * (1f + (PulseMaxScale - 1f) * progress),
                    )
                }
                drawCircle(color = color, radius = radius)
            },
    )
}

/**
 * Status indicator dot colored by a semantic [StatusLevel].
 */
@Composable
fun StatusDot(
    status: StatusLevel,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    size: Dp = AppTheme.dimensions.statusDotSize,
) {
    StatusDot(
        color = status.color,
        haloColor = status.haloColor,
        pulsing = pulsing,
        size = size,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StatusDotPreview() {
    AppTheme {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(status = StatusLevel.Operational)
            StatusDot(status = StatusLevel.Degraded)
            StatusDot(status = StatusLevel.Critical, pulsing = true)
            StatusDot(color = AppTheme.colors.telemetryB, pulsing = true)
        }
    }
}
