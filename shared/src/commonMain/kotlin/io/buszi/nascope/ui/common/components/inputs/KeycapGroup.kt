package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.LocalPlatform
import io.buszi.nascope.OperatingSystem
import io.buszi.nascope.Platform
import io.buszi.nascope.PlatformType
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Renders an inline group of [Keycap] indicators for a keyboard shortcut.
 *
 * Automatically adapts platform-specific modifier keys (e.g. Command on macOS, Ctrl on Windows/Linux)
 * and suppresses display on mobile platforms when [hideOnMobile] is `true`.
 *
 * @param keys The ordered sequence of keys in the shortcut.
 * @param modifier Root layout modifier applied to the row container.
 * @param hideOnMobile Whether to hide the entire shortcut group when running on mobile.
 * @param spacing Inter-keycap spacing cadence, defaults to [AppTheme.spacing.space2xs].
 * @param backgroundColor Fill color passed to each [Keycap].
 * @param borderColor Border color passed to each [Keycap].
 * @param textColor Text color passed to each [Keycap].
 * @param shape Corner radius shape passed to each [Keycap].
 */
@Composable
fun KeycapGroup(
    keys: List<ShortcutKey>,
    modifier: Modifier = Modifier,
    hideOnMobile: Boolean = true,
    spacing: Dp = AppTheme.spacing.space2xs,
    backgroundColor: Color = AppTheme.colors.elevatedOverlay,
    borderColor: Color = AppTheme.colors.elevatedBorder,
    textColor: Color = AppTheme.colors.textSecondary,
    shape: Shape = AppTheme.shapes.base,
) {
    if (hideOnMobile && LocalPlatform.current.isMobile) {
        return
    }

    val platform = LocalPlatform.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { shortcutKey ->
            Keycap(
                key = shortcutKey.resolve(platform),
                hideOnMobile = hideOnMobile,
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                textColor = textColor,
                shape = shape,
            )
        }
    }
}

/**
 * Convenience overload accepting a [KeyCombination].
 */
@Composable
fun KeycapGroup(
    combination: KeyCombination,
    modifier: Modifier = Modifier,
    hideOnMobile: Boolean = true,
    spacing: Dp = AppTheme.spacing.space2xs,
    backgroundColor: Color = AppTheme.colors.elevatedOverlay,
    borderColor: Color = AppTheme.colors.elevatedBorder,
    textColor: Color = AppTheme.colors.textSecondary,
    shape: Shape = AppTheme.shapes.base,
) = KeycapGroup(
    keys = combination.keys,
    modifier = modifier,
    hideOnMobile = hideOnMobile,
    spacing = spacing,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    textColor = textColor,
    shape = shape,
)

/**
 * Convenience vararg overload accepting multiple [ShortcutKey]s.
 */
@Composable
fun KeycapGroup(
    vararg keys: ShortcutKey,
    modifier: Modifier = Modifier,
    hideOnMobile: Boolean = true,
    spacing: Dp = AppTheme.spacing.space2xs,
) = KeycapGroup(
    keys = keys.toList(),
    modifier = modifier,
    hideOnMobile = hideOnMobile,
    spacing = spacing,
)

@Preview
@Composable
private fun KeycapGroupPreview() {
    val macPlatform = object : Platform {
        override val name: String = "macOS Sequoia"
        override val type: PlatformType = PlatformType.Desktop
        override val os: OperatingSystem = OperatingSystem.MacOS
    }

    val windowsPlatform = object : Platform {
        override val name: String = "Windows 11"
        override val type: PlatformType = PlatformType.Desktop
        override val os: OperatingSystem = OperatingSystem.Windows
    }

    val mobilePlatform = object : Platform {
        override val name: String = "Android 15"
        override val type: PlatformType = PlatformType.Android
        override val os: OperatingSystem = OperatingSystem.Android
    }

    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            // macOS preview
            AppTheme(platform = macPlatform) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "macOS Command Palette:",
                        style = AppTheme.typography.bodySm,
                        color = AppTheme.colors.textSecondary,
                    )
                    KeycapGroup(KeyCombination.commandOrControl("K"))
                }
            }

            // Windows/Linux preview
            AppTheme(platform = windowsPlatform) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Windows / Linux Command Palette:",
                        style = AppTheme.typography.bodySm,
                        color = AppTheme.colors.textSecondary,
                    )
                    KeycapGroup(KeyCombination.commandOrControl("K"))
                }
            }

            // Mobile preview (suppressed)
            AppTheme(platform = mobilePlatform) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mobile Preview (Auto-hidden):",
                        style = AppTheme.typography.bodySm,
                        color = AppTheme.colors.textMuted,
                    )
                    KeycapGroup(KeyCombination.commandOrControl("K"))
                }
            }
        }
    }
}
