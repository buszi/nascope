package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.buszi.nascope.ui.common.theme.AppTheme

private const val ShimmerMinAlpha = 0.35f
private const val ShimmerMaxAlpha = 0.85f
private const val ShimmerDurationMillis = 1100

/**
 * Technical pulsing shimmer skeleton box for telemetry loading states.
 *
 * Adheres strictly to the Obsidian Telemetry design system using [AppTheme.colors.elevatedOverlay]
 * and [AppTheme.shapes.base].
 *
 * @param modifier Layout modifier applied to the shimmer box.
 * @param shape Corner radius shape, defaulting to [AppTheme.shapes.base].
 * @param baseColor Base color for the skeleton element, defaulting to [AppTheme.colors.elevatedOverlay].
 */
@Composable
fun WidgetSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.base,
    baseColor: Color = AppTheme.colors.elevatedOverlay,
) {
    val transition = rememberInfiniteTransition(label = "WidgetSkeletonShimmer")
    val alpha by transition.animateFloat(
        initialValue = ShimmerMinAlpha,
        targetValue = ShimmerMaxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "WidgetSkeletonAlpha",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor.copy(alpha = alpha), shape),
    )
}
