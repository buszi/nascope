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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme

private const val DisabledInputAlpha = 0.5f

/**
 * Technical single-line input field (32dp height) engineered for IP addresses, CIDRs,
 * server hostnames, port numbers, and filter queries.
 *
 * Implements an OLED-optimized [AppTheme.colors.inputBackground] fill, hairline border,
 * Electric Violet focus ring ([AppTheme.colors.telemetryA]), and monospace JetBrains Mono typography.
 *
 * @param value Current text string rendered inside the input.
 * @param onValueChange Callback invoked when the user types or modifies text.
 * @param modifier Root layout modifier applied to the input container.
 * @param enabled Controls interactive capability, dimming background and text when `false`.
 * @param readOnly Controls whether the field is editable while still focusable and selectable.
 * @param placeholder Optional hint displayed when [value] is empty.
 * @param leadingIcon Optional leading slot (e.g. search magnifying glass or protocol indicator).
 * @param trailingIcon Optional trailing slot (e.g. clear button or [Keycap] shortcut).
 * @param isError Highlights border with [errorBorderColor] to signal validation failure.
 * @param singleLine Enforces single-line input mode when `true`.
 * @param maxLines Maximum number of visible text lines.
 * @param visualTransformation Formats input display (e.g. password masking or port formatting).
 * @param keyboardOptions Keyboard configuration (IME actions, input types).
 * @param keyboardActions Callbacks triggered by keyboard IME actions.
 * @param interactionSource Interaction stream tracking focus and hover states.
 * @param shape Corner radius shape, defaults to 4dp [AppTheme.shapes.base].
 * @param backgroundColor Fill color, defaults to [AppTheme.colors.inputBackground].
 * @param borderColor Hairline border color in resting state.
 * @param focusedBorderColor Focus ring color on keyboard or pointer focus ([AppTheme.colors.telemetryA]).
 * @param errorBorderColor Validation error border color ([AppTheme.colors.critical]).
 * @param textColor Input text color in active state.
 * @param placeholderColor Hint text color in active state.
 */
@Composable
fun TelemetryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = AppTheme.shapes.base,
    backgroundColor: Color = AppTheme.colors.inputBackground,
    borderColor: Color = AppTheme.colors.baseBorder,
    focusedBorderColor: Color = AppTheme.colors.telemetryA,
    errorBorderColor: Color = AppTheme.colors.critical,
    textColor: Color = AppTheme.colors.textPrimary,
    placeholderColor: Color = AppTheme.colors.textMuted,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.subtleDivider
            isError -> errorBorderColor
            isFocused -> focusedBorderColor
            isHovered -> AppTheme.colors.focusHighlight
            else -> borderColor
        },
        label = "TelemetryTextFieldBorder",
    )

    val activeBackground = if (enabled) {
        backgroundColor
    } else {
        backgroundColor.copy(alpha = DisabledInputAlpha)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(AppTheme.dimensions.inputHeight)
            .background(color = activeBackground, shape = shape)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = animatedBorderColor,
                shape = shape,
            ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = AppTheme.typography.codeBase.copy(
            color = if (enabled) textColor else AppTheme.colors.textDisabled,
        ),
        cursorBrush = SolidColor(if (isError) errorBorderColor else focusedBorderColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = AppTheme.typography.codeBase,
                            color = if (enabled) placeholderColor else AppTheme.colors.textDisabled,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }

                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        },
    )
}

@Preview
@Composable
private fun TelemetryTextFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            TelemetryTextField(
                value = "192.168.1.120",
                onValueChange = {},
                placeholder = "Target IP / Hostname",
                modifier = Modifier.fillMaxWidth(),
            )

            TelemetryTextField(
                value = "",
                onValueChange = {},
                placeholder = "61208 (Default Glances Port)",
                modifier = Modifier.fillMaxWidth(),
            )

            TelemetryTextField(
                value = "https://invalid-host:port",
                onValueChange = {},
                isError = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TelemetryTextField(
                value = "systemd-journald.service",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
