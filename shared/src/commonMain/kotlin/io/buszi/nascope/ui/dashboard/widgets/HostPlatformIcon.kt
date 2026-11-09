package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Technical vector icon representing a host system's operating platform.
 *
 * Rendered using precision canvas drawing adhering to the Obsidian Telemetry
 * high-contrast instrument aesthetic.
 *
 * @param platform Operating system / platform specification.
 * @param modifier Layout modifier applied to the icon canvas.
 * @param tint Accent color used to draw the platform glyph. Defaults to [AppColors.textPrimary].
 */
@Composable
fun HostPlatformIcon(
    platform: HostPlatform,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
) {
    val hairlineBorderWidth = AppTheme.dimensions.hairlineBorder
    val focusRingWidth = AppTheme.dimensions.focusRing

    Canvas(
        modifier = modifier.size(AppTheme.spacing.spaceXl),
    ) {
        val strokeWidth = focusRingWidth.toPx()
        val hairline = hairlineBorderWidth.toPx()

        when (platform) {
            HostPlatform.Linux -> drawLinuxGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.Ubuntu -> drawUbuntuGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.Debian -> drawDebianGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.Synology -> drawSynologyGlyph(tint = tint, hairline = hairline)
            HostPlatform.TrueNas -> drawTrueNasGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.FreeBsd -> drawFreeBsdGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.MacOS -> drawMacOsGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.Windows -> drawWindowsGlyph(tint = tint, strokeWidth = strokeWidth)
            HostPlatform.Docker -> drawDockerGlyph(tint = tint, hairline = hairline)
            HostPlatform.Generic -> drawGenericGlyph(tint = tint, strokeWidth = strokeWidth)
        }
    }
}

private fun DrawScope.drawLinuxGlyph(tint: Color, strokeWidth: Float) {
    val chevronPath = Path().apply {
        moveTo(size.width * 0.16f, size.height * 0.28f)
        lineTo(size.width * 0.44f, size.height * 0.50f)
        lineTo(size.width * 0.16f, size.height * 0.72f)
    }
    drawPath(
        path = chevronPath,
        color = tint,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.52f, size.height * 0.72f),
        end = Offset(size.width * 0.84f, size.height * 0.72f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawUbuntuGlyph(tint: Color, strokeWidth: Float) {
    val centerOffset = Offset(size.width * 0.5f, size.height * 0.5f)
    val ringRadius = size.minDimension * 0.24f
    drawCircle(
        color = tint,
        radius = ringRadius,
        center = centerOffset,
        style = Stroke(width = strokeWidth),
    )
    val dotRadius = size.minDimension * 0.07f
    val orbit = size.minDimension * 0.38f
    drawCircle(
        color = tint,
        radius = dotRadius,
        center = Offset(centerOffset.x, centerOffset.y - orbit),
    )
    drawCircle(
        color = tint,
        radius = dotRadius,
        center = Offset(centerOffset.x - orbit * 0.866f, centerOffset.y + orbit * 0.5f),
    )
    drawCircle(
        color = tint,
        radius = dotRadius,
        center = Offset(centerOffset.x + orbit * 0.866f, centerOffset.y + orbit * 0.5f),
    )
}

private fun DrawScope.drawDebianGlyph(tint: Color, strokeWidth: Float) {
    val swirl = Path().apply {
        moveTo(size.width * 0.48f, size.height * 0.52f)
        cubicTo(
            size.width * 0.35f, size.height * 0.40f,
            size.width * 0.65f, size.height * 0.25f,
            size.width * 0.75f, size.height * 0.50f,
        )
        cubicTo(
            size.width * 0.85f, size.height * 0.75f,
            size.width * 0.35f, size.height * 0.85f,
            size.width * 0.20f, size.height * 0.60f,
        )
        cubicTo(
            size.width * 0.12f, size.height * 0.40f,
            size.width * 0.25f, size.height * 0.18f,
            size.width * 0.55f, size.height * 0.15f,
        )
    }
    drawPath(
        path = swirl,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawSynologyGlyph(tint: Color, hairline: Float) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
        size = Size(size.width * 0.70f, size.height * 0.70f),
        cornerRadius = CornerRadius(size.width * 0.08f),
        style = Stroke(width = hairline * 1.5f),
    )
    val bayWidth = size.width * 0.22f
    val bayHeight = size.height * 0.46f
    val bayY = size.height * 0.24f
    drawRect(
        color = tint.copy(alpha = 0.35f),
        topLeft = Offset(size.width * 0.24f, bayY),
        size = Size(bayWidth, bayHeight),
    )
    drawRect(
        color = tint.copy(alpha = 0.35f),
        topLeft = Offset(size.width * 0.54f, bayY),
        size = Size(bayWidth, bayHeight),
    )
    val ledRadius = size.width * 0.035f
    drawCircle(
        color = tint,
        radius = ledRadius,
        center = Offset(size.width * 0.35f, size.height * 0.77f),
    )
    drawCircle(
        color = tint,
        radius = ledRadius,
        center = Offset(size.width * 0.65f, size.height * 0.77f),
    )
}

private fun DrawScope.drawTrueNasGlyph(tint: Color, strokeWidth: Float) {
    val shelfHeight = size.height * 0.14f
    for (i in 0..2) {
        val top = size.height * (0.20f + i * 0.24f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.15f, top),
            size = Size(size.width * 0.70f, shelfHeight),
            cornerRadius = CornerRadius(size.width * 0.04f),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.035f,
            center = Offset(size.width * 0.75f, top + shelfHeight * 0.5f),
        )
    }
}

private fun DrawScope.drawFreeBsdGlyph(tint: Color, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.20f, size.height * 0.25f)
        cubicTo(
            size.width * 0.20f, size.height * 0.55f,
            size.width * 0.45f, size.height * 0.65f,
            size.width * 0.50f, size.height * 0.65f,
        )
        cubicTo(
            size.width * 0.55f, size.height * 0.65f,
            size.width * 0.80f, size.height * 0.55f,
            size.width * 0.80f, size.height * 0.25f,
        )
    }
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.50f, size.height * 0.35f),
        end = Offset(size.width * 0.50f, size.height * 0.80f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMacOsGlyph(tint: Color, strokeWidth: Float) {
    val applePath = Path().apply {
        moveTo(size.width * 0.50f, size.height * 0.32f)
        cubicTo(
            size.width * 0.35f, size.height * 0.22f,
            size.width * 0.18f, size.height * 0.35f,
            size.width * 0.18f, size.height * 0.60f,
        )
        cubicTo(
            size.width * 0.18f, size.height * 0.82f,
            size.width * 0.38f, size.height * 0.88f,
            size.width * 0.50f, size.height * 0.82f,
        )
        cubicTo(
            size.width * 0.62f, size.height * 0.88f,
            size.width * 0.82f, size.height * 0.82f,
            size.width * 0.82f, size.height * 0.60f,
        )
        cubicTo(
            size.width * 0.82f, size.height * 0.35f,
            size.width * 0.65f, size.height * 0.22f,
            size.width * 0.50f, size.height * 0.32f,
        )
        close()
    }
    drawPath(
        path = applePath,
        color = tint,
        style = Stroke(width = strokeWidth),
    )
    val leaf = Path().apply {
        moveTo(size.width * 0.50f, size.height * 0.28f)
        cubicTo(
            size.width * 0.55f, size.height * 0.14f,
            size.width * 0.68f, size.height * 0.14f,
            size.width * 0.68f, size.height * 0.14f,
        )
        cubicTo(
            size.width * 0.60f, size.height * 0.28f,
            size.width * 0.50f, size.height * 0.28f,
            size.width * 0.50f, size.height * 0.28f,
        )
        close()
    }
    drawPath(path = leaf, color = tint)
}

private fun DrawScope.drawWindowsGlyph(tint: Color, strokeWidth: Float) {
    val halfGap = strokeWidth * 0.8f
    val midX = size.width * 0.5f
    val midY = size.height * 0.5f
    val left = size.width * 0.18f
    val right = size.width * 0.82f
    val top = size.height * 0.18f
    val bottom = size.height * 0.82f

    drawRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(midX - halfGap - left, midY - halfGap - top),
    )
    drawRect(
        color = tint,
        topLeft = Offset(midX + halfGap, top),
        size = Size(right - midX - halfGap, midY - halfGap - top),
    )
    drawRect(
        color = tint,
        topLeft = Offset(left, midY + halfGap),
        size = Size(midX - halfGap - left, bottom - midY - halfGap),
    )
    drawRect(
        color = tint,
        topLeft = Offset(midX + halfGap, midY + halfGap),
        size = Size(right - midX - halfGap, bottom - midY - halfGap),
    )
}

private fun DrawScope.drawDockerGlyph(tint: Color, hairline: Float) {
    val boxW = size.width * 0.14f
    val boxH = size.height * 0.10f
    for (row in 0..1) {
        for (col in 0..2) {
            drawRect(
                color = tint,
                topLeft = Offset(
                    size.width * (0.24f + col * 0.18f),
                    size.height * (0.26f + row * 0.14f),
                ),
                size = Size(boxW, boxH),
            )
        }
    }
    val hull = Path().apply {
        moveTo(size.width * 0.14f, size.height * 0.56f)
        lineTo(size.width * 0.86f, size.height * 0.56f)
        cubicTo(
            size.width * 0.86f, size.height * 0.78f,
            size.width * 0.55f, size.height * 0.82f,
            size.width * 0.25f, size.height * 0.74f,
        )
        close()
    }
    drawPath(
        path = hull,
        color = tint,
        style = Stroke(width = hairline * 2f),
    )
}

private fun DrawScope.drawGenericGlyph(tint: Color, strokeWidth: Float) {
    for (i in 0..2) {
        val top = size.height * (0.20f + i * 0.24f)
        val h = size.height * 0.16f
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.15f, top),
            size = Size(size.width * 0.70f, h),
            cornerRadius = CornerRadius(size.width * 0.04f),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.03f,
            center = Offset(size.width * 0.70f, top + h * 0.5f),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.03f,
            center = Offset(size.width * 0.77f, top + h * 0.5f),
        )
    }
}

@Preview
@Composable
private fun HostPlatformIconPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            Text(
                text = "HOST PLATFORM GLYPHS",
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                HostPlatform.entries.forEach { platform ->
                    Box(
                        modifier = Modifier
                            .size(AppTheme.dimensions.buttonHeightDense)
                            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                            .border(
                                width = AppTheme.dimensions.hairlineBorder,
                                color = AppTheme.colors.baseBorder,
                                shape = AppTheme.shapes.base,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        HostPlatformIcon(
                            platform = platform,
                            tint = AppTheme.colors.telemetryA,
                        )
                    }
                }
            }
        }
    }
}
