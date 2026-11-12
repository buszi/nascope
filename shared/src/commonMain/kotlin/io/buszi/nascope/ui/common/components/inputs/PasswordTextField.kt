package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget

/**
 * [TelemetryTextField] for secrets: the text is masked until the user reveals it with the
 * trailing show/hide action, which is focusable and clickable with keyboard and pointer.
 *
 * Reveal state is local to the field and survives configuration changes, but it is hidden again
 * when the field leaves the composition. Grown to the minimum touch height on Compact windows.
 *
 * @param value Current password text.
 * @param onValueChange Invoked when the user edits the text.
 * @param showLabel Label of the trailing action while the text is masked, e.g. `"SHOW"`.
 * @param hideLabel Label of the trailing action while the text is revealed, e.g. `"HIDE"`.
 * @param modifier Root layout modifier.
 * @param enabled Controls interactivity; a disabled field cannot be edited or revealed.
 * @param placeholder Optional hint displayed when [value] is empty.
 * @param isError Highlights the border to signal validation failure.
 * @param keyboardOptions Keyboard configuration, defaults to a password keyboard without autocorrect.
 * @param keyboardActions Callbacks triggered by keyboard IME actions.
 * @param interactionSource Interaction stream of the text field (focus and hover).
 * @param toggleColor Color of the trailing action, defaults to [AppTheme.colors.textMuted].
 * @param toggleActiveColor Color of the trailing action while focused or hovered, defaults to [AppTheme.colors.textPrimary].
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    toggleColor: Color = AppTheme.colors.textMuted,
    toggleActiveColor: Color = AppTheme.colors.textPrimary,
) {
    var revealed by rememberSaveable { mutableStateOf(false) }

    TelemetryTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.minimumTouchTarget(),
        enabled = enabled,
        placeholder = placeholder,
        isError = isError,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        trailingIcon = {
            VisibilityToggle(
                text = if (revealed) hideLabel else showLabel,
                onClick = { revealed = !revealed },
                enabled = enabled,
                color = toggleColor,
                activeColor = toggleActiveColor,
            )
        },
    )
}

@Composable
private fun VisibilityToggle(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    activeColor: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val animatedColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.textDisabled
            isFocused || isHovered -> activeColor
            else -> color
        },
        label = "PasswordVisibilityToggleText",
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(AppTheme.shapes.base)
            .background(
                color = if (isFocused && enabled) AppTheme.colors.activeHoverRowFill else Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = AppTheme.spacing.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = animatedColor,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun PasswordTextFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            PasswordTextField(
                value = "",
                onValueChange = {},
                showLabel = "SHOW",
                hideLabel = "HIDE",
                placeholder = "Glances password",
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordTextField(
                value = "correct-horse",
                onValueChange = {},
                showLabel = "SHOW",
                hideLabel = "HIDE",
                interactionSource = rememberFocusedInteractionSource(),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordTextField(
                value = "wrong-password",
                onValueChange = {},
                showLabel = "SHOW",
                hideLabel = "HIDE",
                isError = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordTextField(
                value = "correct-horse",
                onValueChange = {},
                showLabel = "SHOW",
                hideLabel = "HIDE",
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
