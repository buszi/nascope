package io.buszi.nascope.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppDimensions
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.theme.DefaultAppDimensions

/**
 * Semantic width classification inspired by AndroidX WindowWidthSizeClass,
 * mapped to Nascope design token breakpoints ([AppDimensions.mobileBreakpoint] and [AppDimensions.desktopBreakpoint]).
 *
 * Supports both window-level and container-level adaptive layouts.
 */
@Immutable
enum class WindowWidthClass {
    /** Screen or container width < [AppDimensions.mobileBreakpoint] (768dp). */
    Compact,

    /** Screen or container width between [AppDimensions.mobileBreakpoint] and [AppDimensions.desktopBreakpoint] (768dp - 1279dp). */
    Medium,

    /** Screen or container width >= [AppDimensions.desktopBreakpoint] (1280dp). */
    Expanded;

    companion object {
        /**
         * Computes the [WindowWidthClass] for a given [width] using [AppDimensions] breakpoints.
         */
        fun fromWidth(
            width: Dp,
            dimensions: AppDimensions = DefaultAppDimensions,
        ): WindowWidthClass = when {
            width < dimensions.mobileBreakpoint -> Compact
            width < dimensions.desktopBreakpoint -> Medium
            else -> Expanded
        }
    }
}

typealias WindowSizeClass = WindowWidthClass

/**
 * Ambient window-level [WindowWidthClass] updated dynamically on window resize and orientation change.
 */
val LocalWindowWidthClass = compositionLocalOf { WindowWidthClass.Compact }

val LocalWindowSizeClass = LocalWindowWidthClass

/**
 * Resolves the [WindowWidthClass] for this [Dp] width using ambient [AppTheme.dimensions].
 */
@Composable
fun Dp.toWindowWidthClass(): WindowWidthClass =
    WindowWidthClass.fromWidth(this, AppTheme.dimensions)
