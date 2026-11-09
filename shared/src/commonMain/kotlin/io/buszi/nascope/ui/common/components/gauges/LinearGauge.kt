package io.buszi.nascope.ui.common.components.gauges

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

private const val DefaultWarningThreshold: Float = 0.75f
private const val DefaultCriticalThreshold: Float = 0.90f
private const val AnimationDurationMillis: Int = 300

/**
 * Horizontal percentage bar gauge with warning/critical threshold color transitions.
 *
 * Smoothly interpolates bar fill and adapts accent color as [progress] crosses
 * [warningThreshold] and [criticalThreshold]. Optional header row renders a technical
 * category [label] alongside a formatted [valueText] with tabular numerals enabled.
 *
 * @param progress Normalized progress value in range `[0.0, 1.0]`. Values outside are clamped.
 * @param modifier Root layout modifier.
 * @param label Optional category label rendered above the bar (e.g. `"CPU CORE 0"`, `"ROOT FS"`).
 * @param valueText Optional formatted metric readout (e.g. `"74.2%"`, `"12.4 / 16.0 GB"`).
 * @param warningThreshold Progress threshold at or above which [warningColor] is activated (default `0.75`).
 * @param criticalThreshold Progress threshold at or above which [criticalColor] is activated (default `0.90`).
 * @param normalColor Indicator color below [warningThreshold]. Defaults to [AppColors.operational].
 * @param warningColor Indicator color between [warningThreshold] and [criticalThreshold]. Defaults to [AppColors.degraded].
 * @param criticalColor Indicator color at or above [criticalThreshold]. Defaults to [AppColors.critical].
 * @param trackColor Fill color for the background track. Defaults to [AppColors.inputBackground].
 * @param borderColor Border color for the track container. Defaults to [AppColors.baseBorder].
 * @param barHeight Height of the indicator track. Defaults to [AppSpacing.spaceSm] (8dp).
 * @param shape Corner radius shape for track and indicator bar. Defaults to [AppShapes.sm] (2dp).
 */
@Composable
fun LinearGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    valueText: String? = null,
    warningThreshold: Float = DefaultWarningThreshold,
    criticalThreshold: Float = DefaultCriticalThreshold,
    normalColor: Color = AppTheme.colors.operational,
    warningColor: Color = AppTheme.colors.degraded,
    criticalColor: Color = AppTheme.colors.critical,
    trackColor: Color = AppTheme.colors.inputBackground,
    borderColor: Color = AppTheme.colors.baseBorder,
    barHeight: Dp = AppTheme.spacing.spaceSm,
    shape: Shape = AppTheme.shapes.sm,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val targetColor = when {
        clampedProgress >= criticalThreshold -> criticalColor
        clampedProgress >= warningThreshold -> warningColor
        else -> normalColor
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "LinearGaugeColor",
    )

    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "LinearGaugeProgress",
    )

    val hasHeader = label != null || valueText != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        if (hasHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = AppTheme.typography.labelUi,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = AppTheme.typography.labelCode,
                        color = animatedColor,
                        maxLines = 1,
                    )
                }
            }
        }

        // Track bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(color = trackColor, shape = shape)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = borderColor,
                    shape = shape,
                ),
        ) {
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(color = animatedColor, shape = shape),
                )
            }
        }
    }
}

@Preview
@Composable
private fun LinearGaugePreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            // Healthy tier (< 75%)
            LinearGauge(
                progress = 0.428f,
                label = "CPU CORE 0",
                valueText = "42.8%",
            )

            // Warning tier (75% - 90%)
            LinearGauge(
                progress = 0.812f,
                label = "RAM ALLOCATION",
                valueText = "13.0 / 16.0 GB (81.2%)",
            )

            // Critical tier (>= 90%)
            LinearGauge(
                progress = 0.946f,
                label = "STORAGE POOL /dev/nvme0n1",
                valueText = "94.6%",
            )

            // Dense mini gauge without header
            LinearGauge(
                progress = 0.65f,
                barHeight = AppTheme.spacing.spaceXs,
                normalColor = AppTheme.colors.telemetryA,
            )
        }
    }
}
