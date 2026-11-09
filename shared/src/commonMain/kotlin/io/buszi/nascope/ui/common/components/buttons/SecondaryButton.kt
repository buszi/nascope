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
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Minimal neutral button with hairline border and subtle surface hover fill.
 *
 * Designed for auxiliary actions, secondary dialog options, and toolbar commands.
 * Supports [ButtonSize.Dense] (32dp) and [ButtonSize.Regular] (36dp).
 *
 * @param onClick Invoked when the user triggers the action via pointer click or keyboard activation.
 * @param modifier Root layout modifier applied to the button container.
 * @param size Button height cadence, defaulting to [ButtonSize.Dense] (32dp).
 * @param enabled Controls interactive responsiveness and visual active/disabled state.
 * @param interactionSource Interaction stream tracking hover, press, and focus states.
 * @param backgroundColor Resting background fill, defaults to [Color.Transparent].
 * @param hoverBackgroundColor Background fill applied on pointer hover.
 * @param borderColor Hairline border color during resting state.
 * @param hoverBorderColor Hairline border color on pointer hover.
 * @param focusedBorderColor Focus ring color on keyboard navigation focus.
 * @param contentColor Text and icon tint for enabled states.
 * @param disabledContentColor Text and icon tint for disabled states.
 * @param shape Corner radius shape, defaults to 4dp [AppTheme.shapes.base].
 * @param content Composable row slot hosting custom label or icon content.
 */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = Color.Transparent,
    hoverBackgroundColor: Color = AppTheme.colors.activeHoverRowFill,
    borderColor: Color = AppTheme.colors.baseBorder,
    hoverBorderColor: Color = AppTheme.colors.focusHighlight,
    focusedBorderColor: Color = AppTheme.colors.telemetryA,
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
            !enabled -> Color.Transparent
            isPressed -> AppTheme.colors.elevatedOverlay
            isHovered -> hoverBackgroundColor
            else -> backgroundColor
        },
        label = "SecondaryButtonBackground",
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.subtleDivider
            isFocused -> focusedBorderColor
            isHovered -> hoverBorderColor
            else -> borderColor
        },
        label = "SecondaryButtonBorder",
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
 * Text-based overload of [SecondaryButton] supporting optional leading and trailing icon slots.
 *
 * @param text Button label rendered with [AppTheme.typography.bodySm] in medium weight.
 * @param leadingIcon Optional leading composable slot (e.g. icon or badge).
 * @param trailingIcon Optional trailing composable slot (e.g. [io.buszi.nascope.ui.common.components.inputs.Keycap] shortcut).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = Color.Transparent,
    hoverBackgroundColor: Color = AppTheme.colors.activeHoverRowFill,
    borderColor: Color = AppTheme.colors.baseBorder,
    hoverBorderColor: Color = AppTheme.colors.focusHighlight,
    focusedBorderColor: Color = AppTheme.colors.telemetryA,
    contentColor: Color = AppTheme.colors.textPrimary,
    disabledContentColor: Color = AppTheme.colors.textDisabled,
    shape: Shape = AppTheme.shapes.base,
) {
    SecondaryButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
        backgroundColor = backgroundColor,
        hoverBackgroundColor = hoverBackgroundColor,
        borderColor = borderColor,
        hoverBorderColor = hoverBorderColor,
        focusedBorderColor = focusedBorderColor,
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

/**
 * Semantic synonym for [SecondaryButton], matching Material design naming conventions.
 */
@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) = SecondaryButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    size = size,
    enabled = enabled,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
)

/**
 * Slot-based semantic synonym for [SecondaryButton].
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Dense,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = SecondaryButton(
    onClick = onClick,
    modifier = modifier,
    size = size,
    enabled = enabled,
    content = content,
)

@Preview
@Composable
private fun SecondaryButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                SecondaryButton(
                    text = "Export CIDR",
                    onClick = {},
                    size = ButtonSize.Dense,
                )
                SecondaryButton(
                    text = "Settings",
                    onClick = {},
                    size = ButtonSize.Regular,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                OutlinedButton(
                    text = "Search Node",
                    onClick = {},
                )
                SecondaryButton(
                    text = "Archived Pool",
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}
