package io.buszi.nascope.ui.common.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AppTheme(
    colors: AppColors = AppColors(),
    typography: AppTypography = DefaultAppTypography,
    shapes: AppShapes = DefaultAppShapes,
    spacing: AppSpacing = DefaultAppSpacing,
    dimensions: AppDimensions = DefaultAppDimensions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes,
        LocalAppSpacing provides spacing,
        LocalAppDimensions provides dimensions,
    ) {
        MaterialTheme(
            colorScheme = colors.materialColorScheme,
            typography = typography.materialTypography,
            shapes = shapes.materialShapes,
            content = content,
        )
    }
}

/**
 * Provides access to the current theme properties in the composition.
 */
object AppTheme {

    /**
     * Extended semantic colors and component accents.
     */
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    /**
     * Standard Material 3 colorScheme.
     */
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    /**
     * Dual-type typography tokens (Inter + JetBrains Mono).
     */
    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    /**
     * Standard Material 3 typography mapped to design tokens.
     */
    val materialTypography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    /**
     * Corner radiuses and shapes.
     */
    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current

    /**
     * Standard Material 3 shapes.
     */
    val materialShapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    /**
     * Spacing cadence and layout gutters/margins.
     */
    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    /**
     * Structural metrics, breakpoints, and component heights/widths.
     */
    val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimensions.current
}
