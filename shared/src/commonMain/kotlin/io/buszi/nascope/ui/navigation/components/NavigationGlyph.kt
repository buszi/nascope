package io.buszi.nascope.ui.navigation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.TopLevelDestination

/**
 * Semantic glyph types rendered across navigation bars, rails, and top control headers.
 */
@Immutable
enum class NavigationGlyphType {
    BrandMark,
    Dashboard,
    Nodes,
    Settings,
    Back,
}

/**
 * Maps a [TopLevelDestination] to its corresponding [NavigationGlyphType].
 */
fun TopLevelDestination.toGlyphType(): NavigationGlyphType = when (this) {
    TopLevelDestination.Dashboard -> NavigationGlyphType.Dashboard
    TopLevelDestination.Nodes -> NavigationGlyphType.Nodes
    TopLevelDestination.Settings -> NavigationGlyphType.Settings
}

/**
 * Technical vector icon for navigation actions and destinations.
 *
 * Drawn with precision Canvas geometry adhering to the Obsidian Telemetry
 * instrument aesthetic.
 *
 * @param type Which navigation glyph to draw.
 * @param modifier Layout modifier applied to the canvas.
 * @param tint Stroke and fill color for the glyph. Defaults to [AppTheme.colors.textPrimary].
 * @param iconSize Square canvas dimension. Defaults to [AppTheme.dimensions.navIconSize].
 */
@Composable
fun NavigationGlyph(
    type: NavigationGlyphType,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    iconSize: Dp = AppTheme.dimensions.navIconSize,
) {
    val strokeWidthDp = AppTheme.dimensions.focusRing
    val hairlineDp = AppTheme.dimensions.hairlineBorder

    Canvas(
        modifier = modifier.size(iconSize),
    ) {
        val strokeWidth = strokeWidthDp.toPx()
        val hairline = hairlineDp.toPx()

        when (type) {
            NavigationGlyphType.BrandMark -> drawBrandMarkGlyph(tint = tint, strokeWidth = strokeWidth)
            NavigationGlyphType.Dashboard -> drawDashboardGlyph(tint = tint, strokeWidth = strokeWidth)
            NavigationGlyphType.Nodes -> drawNodesGlyph(tint = tint, strokeWidth = strokeWidth, hairline = hairline)
            NavigationGlyphType.Settings -> drawSettingsGlyph(tint = tint, strokeWidth = strokeWidth)
            NavigationGlyphType.Back -> drawBackGlyph(tint = tint, strokeWidth = strokeWidth)
        }
    }
}

private fun DrawScope.drawBrandMarkGlyph(tint: Color, strokeWidth: Float) {
    val frame = Path().apply {
        moveTo(size.width * 0.50f, size.height * 0.10f)
        lineTo(size.width * 0.88f, size.height * 0.30f)
        lineTo(size.width * 0.88f, size.height * 0.70f)
        lineTo(size.width * 0.50f, size.height * 0.90f)
        lineTo(size.width * 0.12f, size.height * 0.70f)
        lineTo(size.width * 0.12f, size.height * 0.30f)
        close()
    }
    drawPath(
        path = frame,
        color = tint,
        style = Stroke(width = strokeWidth, join = StrokeJoin.Round),
    )
    val pulse = Path().apply {
        moveTo(size.width * 0.24f, size.height * 0.52f)
        lineTo(size.width * 0.40f, size.height * 0.52f)
        lineTo(size.width * 0.47f, size.height * 0.34f)
        lineTo(size.width * 0.55f, size.height * 0.68f)
        lineTo(size.width * 0.62f, size.height * 0.52f)
        lineTo(size.width * 0.76f, size.height * 0.52f)
    }
    drawPath(
        path = pulse,
        color = tint,
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawDashboardGlyph(tint: Color, strokeWidth: Float) {
    val cellSize = Size(size.width * 0.32f, size.height * 0.32f)
    val corner = CornerRadius(size.width * 0.06f)
    val positions = listOf(
        Offset(size.width * 0.14f, size.height * 0.14f),
        Offset(size.width * 0.54f, size.height * 0.14f),
        Offset(size.width * 0.14f, size.height * 0.54f),
        Offset(size.width * 0.54f, size.height * 0.54f),
    )
    positions.forEach { topLeft ->
        drawRoundRect(
            color = tint,
            topLeft = topLeft,
            size = cellSize,
            cornerRadius = corner,
            style = Stroke(width = strokeWidth * 0.85f),
        )
    }
}

private fun DrawScope.drawNodesGlyph(tint: Color, strokeWidth: Float, hairline: Float) {
    val bladeHeight = size.height * 0.22f
    val corner = CornerRadius(size.width * 0.05f)
    for (row in 0..1) {
        val top = size.height * (0.20f + row * 0.36f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, top),
            size = Size(size.width * 0.72f, bladeHeight),
            cornerRadius = corner,
            style = Stroke(width = strokeWidth * 0.85f),
        )
        drawCircle(
            color = tint,
            radius = hairline * 1.4f,
            center = Offset(size.width * 0.72f, top + bladeHeight * 0.5f),
        )
    }
}

private fun DrawScope.drawSettingsGlyph(tint: Color, strokeWidth: Float) {
    val rows = listOf(0.26f, 0.50f, 0.74f)
    val knobX = listOf(0.38f, 0.66f, 0.46f)
    val knobRadius = size.minDimension * 0.085f

    rows.forEachIndexed { index, yRatio ->
        val y = size.height * yRatio
        val cx = size.width * knobX[index]
        drawLine(
            color = tint,
            start = Offset(size.width * 0.14f, y),
            end = Offset(cx - knobRadius * 1.2f, y),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(cx + knobRadius * 1.2f, y),
            end = Offset(size.width * 0.86f, y),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = knobRadius,
            center = Offset(cx, y),
            style = Stroke(width = strokeWidth * 0.85f),
        )
    }
}

private fun DrawScope.drawBackGlyph(tint: Color, strokeWidth: Float) {
    val chevron = Path().apply {
        moveTo(size.width * 0.56f, size.height * 0.22f)
        lineTo(size.width * 0.26f, size.height * 0.50f)
        lineTo(size.width * 0.56f, size.height * 0.78f)
    }
    drawPath(
        path = chevron,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.28f, size.height * 0.50f),
        end = Offset(size.width * 0.78f, size.height * 0.50f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

@Preview
@Composable
private fun NavigationGlyphPreview() {
    AppTheme {
        Row(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            NavigationGlyphType.entries.forEach { glyphType ->
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimensions.buttonHeightRegular)
                        .background(AppTheme.colors.subSurface, AppTheme.shapes.base)
                        .border(
                            width = AppTheme.dimensions.hairlineBorder,
                            color = AppTheme.colors.baseBorder,
                            shape = AppTheme.shapes.base,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    NavigationGlyph(
                        type = glyphType,
                        tint = AppTheme.colors.telemetryA,
                    )
                }
            }
        }
    }
}
