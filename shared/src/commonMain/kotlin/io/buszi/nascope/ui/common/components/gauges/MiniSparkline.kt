package io.buszi.nascope.ui.common.components.gauges

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme

private const val SparklineStrokeWidthPx: Float = 1.5f
private const val EntryAnimationDurationMillis: Int = 600
private const val DefaultGradientStartAlpha: Float = 0.22f
private const val DefaultGradientEndAlpha: Float = 0.0f
private const val EndpointHaloRadiusPx: Float = 4.0f
private const val EndpointCoreRadiusPx: Float = 2.0f
private const val EndpointHaloAlpha: Float = 0.35f

/**
 * Canvas-based lightweight sparkline with 1.5px stroke, vertical gradient fill,
 * and animated point entry.
 *
 * Built for rendering high-frequency historical telemetry (30–60 samples) with
 * zero composition overhead during animation.
 *
 * @param data Sequence of numerical values representing chronological telemetry samples.
 * @param modifier Root layout modifier. Defaults to full width with `AppTheme.dimensions.sparklineHeight` (36dp).
 * @param lineColor Stroke color for the sparkline path. Defaults to [AppColors.telemetryA].
 * @param gradientStartColor Top fill color for the vertical gradient. Defaults to 22% opacity of [lineColor].
 * @param gradientEndColor Bottom fill color for the vertical gradient. Defaults to 0% opacity of [lineColor].
 * @param strokeWidthPx Line stroke width in pixels. Defaults to `1.5f` px per design specifications.
 * @param minValue Optional lower bound for scaling. If omitted, derives from [data].
 * @param maxValue Optional upper bound for scaling. If omitted, derives from [data].
 * @param animateEntry Whether to animate the entry reveal when [data] is first populated or changes.
 */
@Composable
fun MiniSparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = AppTheme.colors.telemetryA,
    gradientStartColor: Color = lineColor.copy(alpha = DefaultGradientStartAlpha),
    gradientEndColor: Color = lineColor.copy(alpha = DefaultGradientEndAlpha),
    strokeWidthPx: Float = SparklineStrokeWidthPx,
    minValue: Float? = null,
    maxValue: Float? = null,
    animateEntry: Boolean = true,
) {
    val entryProgress = remember { Animatable(if (animateEntry) 0f else 1f) }

    LaunchedEffect(data, animateEntry) {
        if (animateEntry) {
            entryProgress.snapTo(0f)
            entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = EntryAnimationDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            entryProgress.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.sparklineHeight)
            .then(modifier)
            .drawWithCache {
                if (data.size < 2) {
                    return@drawWithCache onDrawBehind { /* No data to render */ }
                }

                val strokePath = Path()
                val fillPath = Path()

                val minVal = minValue ?: (data.minOrNull() ?: 0f)
                val maxVal = maxValue ?: (data.maxOrNull() ?: 1f)
                val valueRange = if (maxVal - minVal > 0.0001f) maxVal - minVal else 1f

                val halfStroke = strokeWidthPx / 2f
                val usableHeight = (size.height - strokeWidthPx).coerceAtLeast(1f)
                val pointCount = data.size
                val dx = size.width / (pointCount - 1).toFloat()

                // Calculate point positions
                var prevX = 0f
                var prevY = size.height - halfStroke - (((data[0] - minVal) / valueRange).coerceIn(0f, 1f) * usableHeight)
                strokePath.moveTo(prevX, prevY)

                for (i in 1 until pointCount) {
                    val currX = i * dx
                    val normalizedY = ((data[i] - minVal) / valueRange).coerceIn(0f, 1f)
                    val currY = size.height - halfStroke - (normalizedY * usableHeight)

                    // Smooth cubic bezier interpolation with zero horizontal overshoot
                    val midX = (prevX + currX) / 2f
                    strokePath.cubicTo(
                        x1 = midX,
                        y1 = prevY,
                        x2 = midX,
                        y2 = currY,
                        x3 = currX,
                        y3 = currY,
                    )

                    prevX = currX
                    prevY = currY
                }

                val lastPointX = prevX
                val lastPointY = prevY

                fillPath.addPath(strokePath)
                fillPath.lineTo(size.width, size.height)
                fillPath.lineTo(0f, size.height)
                fillPath.close()

                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(gradientStartColor, gradientEndColor),
                    startY = 0f,
                    endY = size.height,
                )

                val strokeStyle = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )

                onDrawBehind {
                    val progress = entryProgress.value
                    if (progress <= 0f) return@onDrawBehind

                    val revealWidth = size.width * progress

                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = revealWidth,
                        bottom = size.height,
                    ) {
                        drawPath(
                            path = fillPath,
                            brush = gradientBrush,
                        )
                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = strokeStyle,
                        )

                        // If the reveal has reached the end point, draw the illuminated telemetry dot
                        if (progress >= 1f) {
                            drawCircle(
                                color = lineColor.copy(alpha = EndpointHaloAlpha),
                                radius = EndpointHaloRadiusPx,
                                center = Offset(lastPointX, lastPointY),
                            )
                            drawCircle(
                                color = lineColor,
                                radius = EndpointCoreRadiusPx,
                                center = Offset(lastPointX, lastPointY),
                            )
                        }
                    }
                }
            },
    )
}

@Preview
@Composable
private fun MiniSparklinePreview() {
    AppTheme {
        // 40 realistic telemetry data points simulating fluctuating network and CPU loads
        val sampleSeriesA = remember {
            listOf(
                22f, 24f, 21f, 28f, 34f, 32f, 30f, 45f, 62f, 58f,
                54f, 48f, 52f, 68f, 74f, 71f, 65f, 59f, 62f, 64f,
                70f, 78f, 82f, 80f, 75f, 69f, 63f, 58f, 52f, 49f,
                45f, 42f, 40f, 44f, 48f, 52f, 50f, 46f, 43f, 42.8f,
            )
        }

        val sampleSeriesB = remember {
            listOf(
                120f, 135f, 140f, 128f, 150f, 180f, 240f, 320f, 410f, 550f,
                620f, 710f, 780f, 820f, 840f, 830f, 810f, 790f, 825f, 842f,
                815f, 780f, 740f, 705f, 690f, 720f, 750f, 800f, 830f, 845f,
                820f, 795f, 780f, 810f, 835f, 840f, 838f, 841f, 843f, 842.6f,
            )
        }

        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            MiniSparkline(
                data = sampleSeriesA,
                lineColor = AppTheme.colors.telemetryA,
                animateEntry = false,
            )

            MiniSparkline(
                data = sampleSeriesB,
                lineColor = AppTheme.colors.telemetryB,
                animateEntry = false,
            )
        }
    }
}
