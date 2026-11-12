package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget

private const val CheckedTrackAlpha = 0.14f
private const val DisabledSwitchAlpha = 0.5f

/**
 * On/off toggle sharing the visual language of [SelectableChip] and [TelemetryTextField]: an
 * [AppTheme.colors.inputBackground] track with a hairline border that turns Electric Violet
 * ([AppTheme.colors.telemetryA]) when on, and a focus ring for keyboard navigation.
 *
 * The whole row (track and [label]) is the touch target, grown to the minimum touch size on Compact windows.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the new value when the user toggles the switch.
 * @param modifier Root layout modifier.
 * @param enabled Controls interactivity; a disabled switch is dimmed.
 * @param label Optional text shown after the track, e.g. `"PASSWORD PROTECTED"`.
 * @param interactionSource Interaction stream tracking focus and hover states.
 * @param checkedColor Border and thumb color while on, defaults to [AppTheme.colors.telemetryA].
 * @param uncheckedColor Thumb color while off, defaults to [AppTheme.colors.textMuted].
 * @param trackColor Track fill while off, defaults to [AppTheme.colors.inputBackground].
 * @param borderColor Track border while off, defaults to [AppTheme.colors.baseBorder].
 * @param labelColor Label color, defaults to [AppTheme.colors.textSecondary].
 */
@Composable
fun TelemetrySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    checkedColor: Color = AppTheme.colors.telemetryA,
    uncheckedColor: Color = AppTheme.colors.textMuted,
    trackColor: Color = AppTheme.colors.inputBackground,
    borderColor: Color = AppTheme.colors.baseBorder,
    labelColor: Color = AppTheme.colors.textSecondary,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val animatedTrackColor by animateColorAsState(
        targetValue = if (checked) checkedColor.copy(alpha = CheckedTrackAlpha) else trackColor,
        label = "TelemetrySwitchTrack",
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isFocused -> AppTheme.colors.focusHighlight
            checked -> checkedColor
            isHovered -> AppTheme.colors.focusHighlight
            else -> borderColor
        },
        label = "TelemetrySwitchBorder",
    )
    val animatedThumbColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        label = "TelemetrySwitchThumb",
    )
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "TelemetrySwitchThumbPosition",
    )

    val trackWidth = AppTheme.dimensions.switchTrackWidth
    val trackHeight = AppTheme.dimensions.switchTrackHeight
    val thumbSize = AppTheme.dimensions.switchThumbSize
    val thumbInset = (trackHeight - thumbSize) / 2
    val thumbTravel = trackWidth - thumbSize - thumbInset * 2

    Row(
        modifier = modifier
            .minimumTouchTarget()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .background(
                    color = if (enabled) animatedTrackColor else animatedTrackColor.copy(alpha = DisabledSwitchAlpha),
                    shape = AppTheme.shapes.full,
                )
                .border(
                    width = if (isFocused && enabled) AppTheme.dimensions.focusRing else AppTheme.dimensions.hairlineBorder,
                    color = if (enabled) animatedBorderColor else AppTheme.colors.subtleDivider,
                    shape = AppTheme.shapes.full,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbInset)
                    .offset { IntOffset(x = (thumbTravel * thumbPosition).roundToPx(), y = 0) }
                    .size(thumbSize)
                    .background(
                        color = if (enabled) animatedThumbColor else AppTheme.colors.textDisabled,
                        shape = AppTheme.shapes.full,
                    ),
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = if (enabled) labelColor else AppTheme.colors.textDisabled,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun TelemetrySwitchPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            TelemetrySwitch(checked = false, onCheckedChange = {}, label = "PASSWORD PROTECTED")
            TelemetrySwitch(checked = true, onCheckedChange = {}, label = "PASSWORD PROTECTED")
            TelemetrySwitch(
                checked = true,
                onCheckedChange = {},
                label = "PASSWORD PROTECTED",
                interactionSource = rememberFocusedInteractionSource(),
            )
            TelemetrySwitch(checked = false, onCheckedChange = {}, label = "PASSWORD PROTECTED", enabled = false)
            TelemetrySwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
