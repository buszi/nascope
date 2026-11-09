package io.buszi.nascope.ui.common.components.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.theme.AppTheme

private const val DisabledBackgroundAlpha = 0.35f

/**
 * Primary action button with Electric Violet fill and hover transition (`#7C3AED`).
 *
 * Implements full keyboard navigation, accessible semantics, animated hover/press/focus states,
 * and high-contrast disabled styling adhering to the Obsidian Telemetry design system.
 *
 * @param onClick Invoked when the user triggers the action via pointer click or keyboard activation.
 * @param modifier Root layout modifier applied to the button container.
 * @param size Button height cadence, defaulting to [ButtonSize.Dense] (32dp).
 * @param enabled Controls interactive responsiveness and visual active/disabled state.
 * @param interactionSource Interaction stream tracking hover, press, and focus states.
 * @param backgroundColor Resting fill color, defaults to [AppTheme.colors.buttonPrimary].
 * @param hoverColor Pointer hover fill color, defaults to [AppTheme.colors.buttonPrimaryHover].
 * @param contentColor Text and icon tint for enabled states.
 * @param disabledContentColor Text and icon tint for disabled states.
 * @param shape Corner radius shape, defaults to 4dp [AppTheme.shapes.base].
 * @param content Composable row slot hosting custom label or icon content.
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = AppTheme.colors.buttonPrimary,
    hoverColor: Color = AppTheme.colors.buttonPrimaryHover,
    contentColor: Color = AppTheme.colors.textPrimary,
    disabledContentColor: Color = AppTheme.colors.textDisabled,
    shape: Shape = AppTheme.shapes.base,
    content: @Composable RowScope.() -> Unit,
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> backgroundColor.copy(alpha = DisabledBackgroundAlpha)
            isPressed || isHovered -> hoverColor
            else -> backgroundColor
        },
        label = "PrimaryButtonBackground",
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isFocused && enabled -> AppTheme.colors.focusHighlight
            else -> Color.Transparent
        },
        label = "PrimaryButtonFocusBorder",
    )

    val horizontalPadding = when (size) {
        ButtonSize.Dense -> AppTheme.spacing.spaceMd
        ButtonSize.Regular -> AppTheme.spacing.spaceLg
    }

    Box(
        modifier = modifier
            .height(size.height)
            .background(color = animatedBackgroundColor, shape = shape)
            .border(
                width = if (isFocused && enabled) AppTheme.dimensions.focusRing else AppTheme.dimensions.hairlineBorder,
                color = animatedBorderColor,
                shape = shape,
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs + AppTheme.spacing.space2xs),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Text-based overload of [PrimaryButton] supporting optional leading and trailing icon slots.
 *
 * @param text Button label rendered with [AppTheme.typography.bodySm] in medium weight.
 * @param leadingIcon Optional leading composable slot (e.g. icon or status indicator).
 * @param trailingIcon Optional trailing composable slot (e.g. keycap shortcut or chevron).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = AppTheme.colors.buttonPrimary,
    hoverColor: Color = AppTheme.colors.buttonPrimaryHover,
    contentColor: Color = AppTheme.colors.textPrimary,
    disabledContentColor: Color = AppTheme.colors.textDisabled,
    shape: Shape = AppTheme.shapes.base,
) {
    PrimaryButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
        backgroundColor = backgroundColor,
        hoverColor = hoverColor,
        contentColor = contentColor,
        disabledContentColor = disabledContentColor,
        shape = shape,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
            text = text,
            style = AppTheme.typography.bodySm.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) contentColor else disabledContentColor,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            trailingIcon()
        }
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                PrimaryButton(
                    text = "Inspect Telemetry",
                    onClick = {},
                    size = ButtonSize.Dense,
                )
                PrimaryButton(
                    text = "Connect Server",
                    onClick = {},
                    size = ButtonSize.Regular,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                PrimaryButton(
                    text = "Live Stream",
                    onClick = {},
                    leadingIcon = {
                        StatusDot(color = AppTheme.colors.operational, pulsing = true)
                    },
                )
                PrimaryButton(
                    text = "Disabled Action",
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}
