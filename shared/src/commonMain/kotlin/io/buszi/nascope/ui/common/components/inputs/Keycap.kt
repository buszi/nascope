package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.LocalPlatform
import io.buszi.nascope.OperatingSystem
import io.buszi.nascope.Platform
import io.buszi.nascope.PlatformType
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Desktop keyboard shortcut indicator (`#181C24` fill, `#282E3E` border).
 *
 * Renders technical key combinations and command shortcut glyphs (e.g. `⌘`, `K`, `Ctrl`, `Esc`).
 * Uses elevated panel overlay fill and elevated border with tabular monospace typography.
 *
 * Automatically suppresses rendering on mobile platforms when [hideOnMobile] is `true`.
 *
 * @param key Keycap text or symbol, e.g. `"⌘"`, `"K"`, `"Ctrl"`, `"Esc"`.
 * @param modifier Root layout modifier applied to the keycap container.
 * @param hideOnMobile Whether to hide this keycap when running on a mobile platform ([LocalPlatform]).
 * @param backgroundColor Fill color, defaults to [AppTheme.colors.elevatedOverlay] (`#181C24`).
 * @param borderColor Border color, defaults to [AppTheme.colors.elevatedBorder] (`#282E3E`).
 * @param textColor Text color, defaults to [AppTheme.colors.textSecondary].
 * @param shape Corner radius shape, defaults to 4dp [AppTheme.shapes.base].
 */
@Composable
fun Keycap(
    key: String,
    modifier: Modifier = Modifier,
    hideOnMobile: Boolean = true,
    backgroundColor: Color = AppTheme.colors.elevatedOverlay,
    borderColor: Color = AppTheme.colors.elevatedBorder,
    textColor: Color = AppTheme.colors.textSecondary,
    shape: Shape = AppTheme.shapes.base,
) {
    if (hideOnMobile && LocalPlatform.current.isMobile) return

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = AppTheme.spacing.spaceLg + AppTheme.spacing.spaceXs)
            .background(color = backgroundColor, shape = shape)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = shape,
            )
            .padding(
                horizontal = AppTheme.spacing.spaceXs + AppTheme.spacing.space2xs,
                vertical = AppTheme.spacing.space2xs,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key,
            style = AppTheme.typography.labelCode,
            color = textColor,
            maxLines = 1,
        )
    }
}

/**
 * Platform-adaptive overload of [Keycap] accepting a [ShortcutKey].
 *
 * Automatically resolves modifier keys (such as [ShortcutKey.CommandOrControl])
 * according to the current host platform in composition ([LocalPlatform]).
 *
 * @param key [ShortcutKey] definition (e.g. [ShortcutKey.CommandOrControl] or [ShortcutKey.Space]).
 */
@Composable
fun Keycap(
    key: ShortcutKey,
    modifier: Modifier = Modifier,
    hideOnMobile: Boolean = true,
    backgroundColor: Color = AppTheme.colors.elevatedOverlay,
    borderColor: Color = AppTheme.colors.elevatedBorder,
    textColor: Color = AppTheme.colors.textSecondary,
    shape: Shape = AppTheme.shapes.base,
) {
    val platform = LocalPlatform.current
    Keycap(
        key = key.resolve(platform),
        modifier = modifier,
        hideOnMobile = hideOnMobile,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        textColor = textColor,
        shape = shape,
    )
}

@Preview
@Composable
private fun KeycapPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Command Palette:",
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textSecondary,
                )
                Keycap(key = "⌘")
                Keycap(key = "K")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quick Refresh:",
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textSecondary,
                )
                Keycap(key = "Ctrl")
                Keycap(key = "R")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Keycap(key = "Esc")
                Keycap(key = "Tab")
                Keycap(key = "Enter")
                Keycap(key = "Shift")
            }

            // Mobile platform preview: shortcuts suppressed by default, or forced visible
            val mockMobilePlatform = object : Platform {
                override val name: String = "Android Mobile"
                override val type: PlatformType = PlatformType.Android
                override val os: OperatingSystem = OperatingSystem.Android
            }
            AppTheme(platform = mockMobilePlatform) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
                    Text(
                        text = "Mobile View (Keycaps Suppressed by Default):",
                        style = AppTheme.typography.labelUi,
                        color = AppTheme.colors.textMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Hidden on Mobile:",
                            style = AppTheme.typography.bodySm,
                            color = AppTheme.colors.textSecondary,
                        )
                        Keycap(key = "⌘")
                        Keycap(key = "K")
                        Text(
                            text = "Explicitly Forced:",
                            style = AppTheme.typography.bodySm,
                            color = AppTheme.colors.textSecondary,
                        )
                        Keycap(key = "Esc", hideOnMobile = false)
                    }
                }
            }
        }
    }
}
