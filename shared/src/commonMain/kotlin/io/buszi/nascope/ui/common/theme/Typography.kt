package io.buszi.nascope.ui.common.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Dual-type system:
 * - [interFontFamily] handles narrative flow, navigation, structure, and buttons.
 * - [monoFontFamily] governs system telemetry, addresses, gauges, and code.
 */
@Immutable
data class AppTypography(
    // Inter styles
    val headlineXl: TextStyle,
    val headlineXlMobile: TextStyle,
    val headlineLg: TextStyle,
    val headlineSm: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val bodySm: TextStyle,
    val labelUi: TextStyle,

    // JetBrains Mono styles (with tabular numerals `tnum`)
    val metricDisplay: TextStyle,
    val metricMd: TextStyle,
    val codeBase: TextStyle,
    val labelCode: TextStyle,

    // Material 3 Typography backing
    val materialTypography: Typography,
)

/**
 * Creates an [AppTypography] instance with the specified font families.
 * By default, falls back to platform SansSerif (Inter) and Monospace (JetBrains Mono).
 */
fun appTypography(
    interFontFamily: FontFamily = FontFamily.SansSerif,
    monoFontFamily: FontFamily = FontFamily.Monospace,
): AppTypography {
    val tnum = "tnum"

    val headlineXl = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 36.sp, // 2.25rem
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 44.sp, // 2.75rem
        letterSpacing = (-0.025).em,
    )

    val headlineXlMobile = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 28.sp, // 1.75rem
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 36.sp, // 2.25rem
        letterSpacing = (-0.02).em,
    )

    val headlineLg = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 24.sp, // 1.5rem
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 32.sp, // 2rem
        letterSpacing = (-0.02).em,
    )

    val headlineSm = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 18.sp, // 1.125rem
        fontWeight = FontWeight.Medium, // 500
        lineHeight = 24.sp, // 1.5rem
        letterSpacing = (-0.015).em,
    )

    val bodyLg = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 15.sp, // 0.9375rem
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 24.sp, // 1.5rem
    )

    val bodyMd = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 14.sp, // 0.875rem
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 22.sp, // 1.375rem
    )

    val bodySm = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 12.sp, // 0.75rem
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 18.sp, // 1.125rem
    )

    val labelUi = TextStyle(
        fontFamily = interFontFamily,
        fontSize = 11.sp, // 0.6875rem
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 14.sp, // 0.875rem
        letterSpacing = 0.04.em,
    )

    val metricDisplay = TextStyle(
        fontFamily = monoFontFamily,
        fontSize = 32.sp, // 2rem
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 36.sp, // 2.25rem
        letterSpacing = (-0.03).em,
        fontFeatureSettings = tnum,
    )

    val metricMd = TextStyle(
        fontFamily = monoFontFamily,
        fontSize = 20.sp, // 1.25rem
        fontWeight = FontWeight.Medium, // 500
        lineHeight = 24.sp, // 1.5rem
        letterSpacing = (-0.02).em,
        fontFeatureSettings = tnum,
    )

    val codeBase = TextStyle(
        fontFamily = monoFontFamily,
        fontSize = 13.sp, // 0.8125rem
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 20.sp, // 1.25rem
        fontFeatureSettings = tnum,
    )

    val labelCode = TextStyle(
        fontFamily = monoFontFamily,
        fontSize = 11.sp, // 0.6875rem
        fontWeight = FontWeight.Medium, // 500
        lineHeight = 14.sp, // 0.875rem
        letterSpacing = 0.02.em,
        fontFeatureSettings = tnum,
    )

    val materialTypography = Typography(
        displayLarge = headlineXl,
        displayMedium = metricDisplay,
        displaySmall = headlineXlMobile,
        headlineLarge = headlineLg,
        headlineMedium = metricMd,
        headlineSmall = headlineSm,
        titleLarge = headlineSm,
        titleMedium = bodyLg.copy(fontWeight = FontWeight.Medium),
        titleSmall = bodyMd.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLg,
        bodyMedium = bodyMd,
        bodySmall = bodySm,
        labelLarge = labelUi.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelMedium = labelUi,
        labelSmall = labelCode,
    )

    return AppTypography(
        headlineXl = headlineXl,
        headlineXlMobile = headlineXlMobile,
        headlineLg = headlineLg,
        headlineSm = headlineSm,
        bodyLg = bodyLg,
        bodyMd = bodyMd,
        bodySm = bodySm,
        labelUi = labelUi,
        metricDisplay = metricDisplay,
        metricMd = metricMd,
        codeBase = codeBase,
        labelCode = labelCode,
        materialTypography = materialTypography,
    )
}

val DefaultAppTypography = appTypography()

val LocalAppTypography = staticCompositionLocalOf { DefaultAppTypography }
