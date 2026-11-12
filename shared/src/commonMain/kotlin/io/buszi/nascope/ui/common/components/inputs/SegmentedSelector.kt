package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget

private const val SelectedFillAlpha = 0.14f
private const val DisabledAlpha = 0.5f
private const val MinOptions = 2
private const val MaxOptions = 3

/**
 * Connected segmented control for two or three mutually exclusive options, such as `HTTP` / `HTTPS`.
 *
 * Unlike a row of [SelectableChip]s, the segments share one outline and divide the width equally,
 * so the group reads as a single control. The selected segment uses the same Electric Violet tint as
 * [SelectableChip]. Each segment is focusable and selectable with keyboard and pointer, and the group
 * is exposed to accessibility as one radio group. Grown to the minimum touch height on Compact windows.
 *
 * @param options The 2-3 selectable values.
 * @param selected The currently selected value; should be one of [options].
 * @param onSelect Invoked with the value the user picks.
 * @param optionLabel Maps a value to its label, rendered in code typography.
 * @param modifier Root layout modifier.
 * @param enabled Controls interactivity; a disabled control is dimmed.
 * @param optionTestTag Optional test tag per option, so UI automation can pick a single segment.
 * @param shape Outline shape, defaults to [AppTheme.shapes.base].
 * @param backgroundColor Fill of unselected segments, defaults to [AppTheme.colors.inputBackground].
 * @param borderColor Outline and divider color, defaults to [AppTheme.colors.baseBorder].
 * @param selectedColor Border accent of the selected segment, defaults to [AppTheme.colors.telemetryA].
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionTestTag: (T) -> String? = { null },
    shape: Shape = AppTheme.shapes.base,
    backgroundColor: Color = AppTheme.colors.inputBackground,
    borderColor: Color = AppTheme.colors.baseBorder,
    selectedColor: Color = AppTheme.colors.telemetryA,
) {
    require(options.size in MinOptions..MaxOptions) {
        "SegmentedSelector supports $MinOptions-$MaxOptions options, got ${options.size}"
    }

    Row(
        modifier = modifier
            .minimumTouchTarget()
            .height(AppTheme.dimensions.inputHeight)
            .selectableGroup()
            .background(color = backgroundColor, shape = shape)
            .border(width = AppTheme.dimensions.hairlineBorder, color = borderColor, shape = shape)
            .clip(shape),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(AppTheme.dimensions.hairlineBorder)
                        .fillMaxHeight()
                        .background(borderColor),
                )
            }
            Segment(
                text = optionLabel(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = enabled,
                selectedColor = selectedColor,
                modifier = Modifier
                    .weight(1f)
                    .then(optionTestTag(option)?.let { Modifier.testTag(it) } ?: Modifier),
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val animatedFill by animateColorAsState(
        targetValue = when {
            selected -> selectedColor.copy(alpha = SelectedFillAlpha)
            isHovered -> AppTheme.colors.activeHoverRowFill
            else -> Color.Transparent
        },
        label = "SegmentedSelectorFill",
    )
    val textColor = when {
        !enabled -> AppTheme.colors.textDisabled
        selected -> AppTheme.colors.textPrimary
        else -> AppTheme.colors.textMuted
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (enabled) animatedFill else animatedFill.copy(alpha = animatedFill.alpha * DisabledAlpha))
            .optionalModifier(isFocused && enabled) {
                border(width = AppTheme.dimensions.focusRing, color = AppTheme.colors.focusHighlight)
            }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = AppTheme.spacing.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
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
private fun SegmentedSelectorPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            SegmentedSelector(
                options = listOf("HTTP", "HTTPS"),
                selected = "HTTP",
                onSelect = {},
                optionLabel = { it },
                modifier = Modifier.fillMaxWidth(),
            )
            SegmentedSelector(
                options = listOf("1 MIN", "5 MIN", "15 MIN"),
                selected = "5 MIN",
                onSelect = {},
                optionLabel = { it },
                modifier = Modifier.fillMaxWidth(),
            )
            SegmentedSelector(
                options = listOf("HTTP", "HTTPS"),
                selected = "HTTPS",
                onSelect = {},
                optionLabel = { it },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
