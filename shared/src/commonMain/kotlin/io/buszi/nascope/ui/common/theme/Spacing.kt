package io.buszi.nascope.ui.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * - Micro-spacing ([space2xs] to [spaceSm]): badge padding, indicator offsets, input padding.
 * - Structural spacing ([spaceMd] to [space2xl]): isolating modular panels, telemetry groups, widgets.
 */
@Immutable
data class AppSpacing(
    // Grid gutters & page margins
    val gutter: Dp = 16.dp, // 1rem
    val gutterDesktop: Dp = 20.dp, // 1.25rem
    val margin: Dp = 16.dp, // 1rem
    val marginTablet: Dp = 24.dp, // 1.5rem
    val marginDesktop: Dp = 32.dp, // 2rem

    // Cadence scale
    val space2xs: Dp = 2.dp, // 0.125rem
    val spaceXs: Dp = 4.dp, // 0.25rem
    val spaceSm: Dp = 8.dp, // 0.5rem
    val spaceMd: Dp = 12.dp, // 0.75rem
    val spaceLg: Dp = 16.dp, // 1rem
    val spaceXl: Dp = 24.dp, // 1.5rem
    val space2xl: Dp = 32.dp, // 2rem
)

@Immutable
data class AppDimensions(
    // Borders
    val hairlineBorder: Dp = 1.dp,
    val focusRing: Dp = 2.dp,
    val focusRingOffset: Dp = 2.dp,

    // Component sizes
    val statusDotSize: Dp = 6.dp,
    val checkboxSize: Dp = 14.dp,
    val buttonHeightDense: Dp = 32.dp,
    val buttonHeightRegular: Dp = 36.dp,
    val inputHeight: Dp = 32.dp,
    val tableHeaderHeight: Dp = 28.dp,
    val sparklineHeight: Dp = 36.dp,

    // Layout engine metrics
    val sidebarDesktopWidth: Dp = 240.dp,
    val sidebarTabletWidth: Dp = 56.dp,
    val liveLogDrawerWidth: Dp = 320.dp,
    val commandPaletteWidth: Dp = 580.dp,

    // Dashboard modular brick height unit (1U)
    val brickHeightUnit: Dp = 250.dp,

    // Breakpoints
    val mobileBreakpoint: Dp = 768.dp,
    val desktopBreakpoint: Dp = 1280.dp,
)

val DefaultAppSpacing = AppSpacing()
val DefaultAppDimensions = AppDimensions()

val LocalAppSpacing = staticCompositionLocalOf { DefaultAppSpacing }
val LocalAppDimensions = staticCompositionLocalOf { DefaultAppDimensions }
