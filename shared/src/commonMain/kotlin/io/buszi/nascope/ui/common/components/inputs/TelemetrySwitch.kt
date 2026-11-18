package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget

private const val CheckedTrackAlpha = 0.14f
private const val DisabledSwitchAlpha = 0.5f

/**
 * On/off toggle: Material 3's [Switch] (its thumb motion, press and hover / focus state layer) in the visual language
 * of [SelectableChip] and [TelemetryTextField]: an [AppTheme.colors.inputBackground] track whose border turns Electric
 * Violet ([AppTheme.colors.telemetryA]) when on, and turns [AppTheme.colors.focusHighlight] on hover and keyboard focus.
 *
 * The whole row (switch and [label]) is the touch target, grown to the minimum touch size on Compact windows only;
 * the [Switch] itself only draws (`onCheckedChange = null`) and follows the row's [interactionSource].
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the new value when the user toggles the switch.
 * @param modifier Root layout modifier.
 * @param enabled Controls interactivity; a disabled switch is dimmed.
 * @param label Optional text shown after the switch, e.g. `"PASSWORD PROTECTED"`.
 * @param interactionSource Interaction stream tracking press, focus and hover states.
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
    val highlight = AppTheme.colors.focusHighlight

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
        // The state layer around the thumb takes the content color; the default would be invisible on our surfaces.
        CompositionLocalProvider(LocalContentColor provides if (checked) checkedColor else AppTheme.colors.textPrimary) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                interactionSource = interactionSource,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = checkedColor,
                    checkedTrackColor = checkedColor.copy(alpha = CheckedTrackAlpha),
                    checkedBorderColor = if (isFocused) highlight else checkedColor,
                    uncheckedThumbColor = uncheckedColor,
                    uncheckedTrackColor = trackColor,
                    uncheckedBorderColor = if (isFocused || isHovered) highlight else borderColor,
                    disabledCheckedThumbColor = AppTheme.colors.textDisabled,
                    disabledCheckedTrackColor = checkedColor.copy(alpha = CheckedTrackAlpha * DisabledSwitchAlpha),
                    disabledCheckedBorderColor = AppTheme.colors.subtleDivider,
                    disabledUncheckedThumbColor = AppTheme.colors.textDisabled,
                    disabledUncheckedTrackColor = trackColor.copy(alpha = DisabledSwitchAlpha),
                    disabledUncheckedBorderColor = AppTheme.colors.subtleDivider,
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
