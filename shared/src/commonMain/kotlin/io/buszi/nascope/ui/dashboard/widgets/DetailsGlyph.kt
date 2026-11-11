package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * North-east arrow shown in a brick header to signal that the brick can be opened for details.
 *
 * @param tint Stroke color; muted when details are merely available, accented when open.
 * @param iconSize Square canvas dimension.
 */
@Composable
fun DetailsGlyph(
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textMuted,
    iconSize: Dp = 14.dp,
) {
    val strokeWidthDp = AppTheme.dimensions.focusRing

    Canvas(modifier = modifier.size(iconSize)) {
        val stroke = Stroke(
            width = strokeWidthDp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.22f, size.height * 0.78f),
            end = Offset(size.width * 0.78f, size.height * 0.22f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(size.width * 0.34f, size.height * 0.22f)
            lineTo(size.width * 0.78f, size.height * 0.22f)
            lineTo(size.width * 0.78f, size.height * 0.66f)
        }
        drawPath(path = head, color = tint, style = stroke)
    }
}

@Preview
@Composable
private fun DetailsGlyphPreview() {
    AppTheme {
        DetailsGlyph()
    }
}
