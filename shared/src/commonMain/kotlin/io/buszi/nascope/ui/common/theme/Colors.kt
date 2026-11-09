package io.buszi.nascope.ui.common.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal object Palette {
    val DeepObsidianVoid = Color(0xFF0B0F17)
    val ZincSurface = Color(0xFF12141A)
    val ElevatedSurface = Color(0xFF181C24)
    val TranslucentTint = Color(0x401E232F)
    val InputFill = Color(0xFF0E121A)

    val HairlineBorder = Color(0xFF1E232F)
    val SubtleDivider = Color(0xFF171B26)
    val FocusHighlight = Color(0xFF384259)
    val ElevatedBorder = Color(0xFF282E3E)

    val EmeraldGreen = Color(0xFF10B981)
    val EmeraldHalo = Color(0x1F10B981)
    val EmeraldBadgeBg = Color(0x1410B981)
    val EmeraldBadgeBorder = Color(0x3310B981)

    val Amber = Color(0xFFF59E0B)
    val AmberHalo = Color(0x1FF59E0B)
    val AmberBadgeBg = Color(0x14F59E0B)
    val AmberBadgeBorder = Color(0x33F59E0B)

    val Rose = Color(0xFFF43F5E)
    val RoseHalo = Color(0x1FF43F5E)
    val RoseBadgeBg = Color(0x14F43F5E)
    val RoseBadgeBorder = Color(0x33F43F5E)

    val ElectricViolet = Color(0xFF8B5CF6)
    val ElectricVioletHover = Color(0xFF7C3AED)
    val ElectricVioletHalo = Color(0x668B5CF6)
    val Cyan = Color(0xFF06B6D4)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val TextDisabled = Color(0xFF334155)

    // Sparkline gradients
    val SparklineGradientStart = Color(0x228B5CF6)
    val SparklineGradientEnd = Color(0x008B5CF6)

    // Overlays
    val CommandPaletteOverlay = Color(0xCC12141A)
    val ToastOverlay = Color(0xCC181C24)
    val HoverRowFill = Color(0x80181C24)
    val GhostHoverFill = Color(0x801E232F)

    // Material 3 Dark Palette Hex tokens
    val M3Surface = Color(0xFF0F131C)
    val M3SurfaceDim = Color(0xFF0F131C)
    val M3SurfaceBright = Color(0xFF353942)
    val M3SurfaceContainerLowest = Color(0xFF0A0E16)
    val M3SurfaceContainerLow = Color(0xFF181C24)
    val M3SurfaceContainer = Color(0xFF1C2028)
    val M3SurfaceContainerHigh = Color(0xFF262A33)
    val M3SurfaceContainerHighest = Color(0xFF31353E)
    val M3OnSurface = Color(0xFFDFE2EE)
    val M3OnSurfaceVariant = Color(0xFFCBC3D7)
    val M3InverseSurface = Color(0xFFDFE2EE)
    val M3InverseOnSurface = Color(0xFF2C3039)
    val M3Outline = Color(0xFF958EA0)
    val M3OutlineVariant = Color(0xFF494454)
    val M3SurfaceTint = Color(0xFFD0BCFF)
    val M3Primary = Color(0xFFD0BCFF)
    val M3OnPrimary = Color(0xFF3C0091)
    val M3PrimaryContainer = Color(0xFFA078FF)
    val M3OnPrimaryContainer = Color(0xFF340080)
    val M3InversePrimary = Color(0xFF6D3BD7)
    val M3Secondary = Color(0xFF4CD7F6)
    val M3OnSecondary = Color(0xFF003640)
    val M3SecondaryContainer = Color(0xFF03B5D3)
    val M3OnSecondaryContainer = Color(0xFF00424E)
    val M3Tertiary = Color(0xFF4EDEA3)
    val M3OnTertiary = Color(0xFF003824)
    val M3TertiaryContainer = Color(0xFF00A572)
    val M3OnTertiaryContainer = Color(0xFF00311F)
    val M3Error = Color(0xFFFFB4AB)
    val M3OnError = Color(0xFF690005)
    val M3ErrorContainer = Color(0xFF93000A)
    val M3OnErrorContainer = Color(0xFFFFDAD6)
    val M3Background = Color(0xFF0F131C)
    val M3OnBackground = Color(0xFFDFE2EE)
    val M3SurfaceVariant = Color(0xFF31353E)

    // Fixed variants
    val M3PrimaryFixed = Color(0xFFE9DDFF)
    val M3PrimaryFixedDim = Color(0xFFD0BCFF)
    val M3OnPrimaryFixed = Color(0xFF23005C)
    val M3OnPrimaryFixedVariant = Color(0xFF5516BE)
    val M3SecondaryFixed = Color(0xFFACEDFF)
    val M3SecondaryFixedDim = Color(0xFF4CD7F6)
    val M3OnSecondaryFixed = Color(0xFF001F26)
    val M3OnSecondaryFixedVariant = Color(0xFF004E5C)
    val M3TertiaryFixed = Color(0xFF6FFBBE)
    val M3TertiaryFixedDim = Color(0xFF4EDEA3)
    val M3OnTertiaryFixed = Color(0xFF002113)
    val M3OnTertiaryFixedVariant = Color(0xFF005236)
}

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Palette.M3Primary,
    onPrimary = Palette.M3OnPrimary,
    primaryContainer = Palette.M3PrimaryContainer,
    onPrimaryContainer = Palette.M3OnPrimaryContainer,
    inversePrimary = Palette.M3InversePrimary,
    secondary = Palette.M3Secondary,
    onSecondary = Palette.M3OnSecondary,
    secondaryContainer = Palette.M3SecondaryContainer,
    onSecondaryContainer = Palette.M3OnSecondaryContainer,
    tertiary = Palette.M3Tertiary,
    onTertiary = Palette.M3OnTertiary,
    tertiaryContainer = Palette.M3TertiaryContainer,
    onTertiaryContainer = Palette.M3OnTertiaryContainer,
    background = Palette.M3Background,
    onBackground = Palette.M3OnBackground,
    surface = Palette.M3Surface,
    onSurface = Palette.M3OnSurface,
    surfaceVariant = Palette.M3SurfaceVariant,
    onSurfaceVariant = Palette.M3OnSurfaceVariant,
    surfaceTint = Palette.M3SurfaceTint,
    inverseSurface = Palette.M3InverseSurface,
    inverseOnSurface = Palette.M3InverseOnSurface,
    error = Palette.M3Error,
    onError = Palette.M3OnError,
    errorContainer = Palette.M3ErrorContainer,
    onErrorContainer = Palette.M3OnErrorContainer,
    outline = Palette.M3Outline,
    outlineVariant = Palette.M3OutlineVariant,
    scrim = Color(0xFF000000),
    surfaceBright = Palette.M3SurfaceBright,
    surfaceDim = Palette.M3SurfaceDim,
    surfaceContainer = Palette.M3SurfaceContainer,
    surfaceContainerHigh = Palette.M3SurfaceContainerHigh,
    surfaceContainerHighest = Palette.M3SurfaceContainerHighest,
    surfaceContainerLow = Palette.M3SurfaceContainerLow,
    surfaceContainerLowest = Palette.M3SurfaceContainerLowest,
)

@Immutable
data class AppColors(
    // Base & Surface Hierarchy
    val canvasBackground: Color = Palette.DeepObsidianVoid,
    val subSurface: Color = Palette.ZincSurface,
    val elevatedOverlay: Color = Palette.ElevatedSurface,
    val activeHoverRowFill: Color = Palette.TranslucentTint,
    val inputBackground: Color = Palette.InputFill,

    // Border & Division Tokens
    val baseBorder: Color = Palette.HairlineBorder,
    val subtleDivider: Color = Palette.SubtleDivider,
    val focusHighlight: Color = Palette.FocusHighlight,
    val elevatedBorder: Color = Palette.ElevatedBorder,

    // Telemetry Status Tokens
    val operational: Color = Palette.EmeraldGreen,
    val operationalHalo: Color = Palette.EmeraldHalo,
    val degraded: Color = Palette.Amber,
    val degradedHalo: Color = Palette.AmberHalo,
    val critical: Color = Palette.Rose,
    val criticalHalo: Color = Palette.RoseHalo,
    val telemetryA: Color = Palette.ElectricViolet,
    val telemetryB: Color = Palette.Cyan,

    // Monochromatic Text Stack
    val textPrimary: Color = Palette.TextPrimary,
    val textSecondary: Color = Palette.TextSecondary,
    val textMuted: Color = Palette.TextMuted,
    val textDisabled: Color = Palette.TextDisabled,

    // Badges & Component Accents
    val healthyBadgeBackground: Color = Palette.EmeraldBadgeBg,
    val healthyBadgeBorder: Color = Palette.EmeraldBadgeBorder,
    val warningBadgeBackground: Color = Palette.AmberBadgeBg,
    val warningBadgeBorder: Color = Palette.AmberBadgeBorder,
    val criticalBadgeBackground: Color = Palette.RoseBadgeBg,
    val criticalBadgeBorder: Color = Palette.RoseBadgeBorder,
    val buttonPrimary: Color = Palette.ElectricViolet,
    val buttonPrimaryHover: Color = Palette.ElectricVioletHover,
    val commandPaletteOverlay: Color = Palette.CommandPaletteOverlay,
    val toastOverlay: Color = Palette.ToastOverlay,
    val sparklineGradientStart: Color = Palette.SparklineGradientStart,
    val sparklineGradientEnd: Color = Palette.SparklineGradientEnd,

    // Fixed variants
    val primaryFixed: Color = Palette.M3PrimaryFixed,
    val primaryFixedDim: Color = Palette.M3PrimaryFixedDim,
    val onPrimaryFixed: Color = Palette.M3OnPrimaryFixed,
    val onPrimaryFixedVariant: Color = Palette.M3OnPrimaryFixedVariant,
    val secondaryFixed: Color = Palette.M3SecondaryFixed,
    val secondaryFixedDim: Color = Palette.M3SecondaryFixedDim,
    val onSecondaryFixed: Color = Palette.M3OnSecondaryFixed,
    val onSecondaryFixedVariant: Color = Palette.M3OnSecondaryFixedVariant,
    val tertiaryFixed: Color = Palette.M3TertiaryFixed,
    val tertiaryFixedDim: Color = Palette.M3TertiaryFixedDim,
    val onTertiaryFixed: Color = Palette.M3OnTertiaryFixed,
    val onTertiaryFixedVariant: Color = Palette.M3OnTertiaryFixedVariant,

    // Material 3 ColorScheme backing
    val materialColorScheme: ColorScheme = DarkColorScheme,
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }
