package io.buszi.nascope.ui.common.components.badges

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Semantic health level of a telemetry subsystem.
 * Drives the color tokens used by [StatusChip] and [StatusDot].
 */
enum class StatusLevel {
    /** Healthy / nominal operation (Emerald). */
    Operational,

    /** Warning / reduced performance (Amber). */
    Degraded,

    /** Failure / threshold exceeded (Rose). */
    Critical,
}

/** Solid accent color used for the status dot and label text. */
val StatusLevel.color: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        StatusLevel.Operational -> AppTheme.colors.operational
        StatusLevel.Degraded -> AppTheme.colors.degraded
        StatusLevel.Critical -> AppTheme.colors.critical
    }

/** Translucent luminous halo drawn around the status dot. */
val StatusLevel.haloColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        StatusLevel.Operational -> AppTheme.colors.operationalHalo
        StatusLevel.Degraded -> AppTheme.colors.degradedHalo
        StatusLevel.Critical -> AppTheme.colors.criticalHalo
    }

/** Tinted chip background fill. */
val StatusLevel.badgeBackgroundColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        StatusLevel.Operational -> AppTheme.colors.healthyBadgeBackground
        StatusLevel.Degraded -> AppTheme.colors.warningBadgeBackground
        StatusLevel.Critical -> AppTheme.colors.criticalBadgeBackground
    }

/** Tinted chip hairline border. */
val StatusLevel.badgeBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        StatusLevel.Operational -> AppTheme.colors.healthyBadgeBorder
        StatusLevel.Degraded -> AppTheme.colors.warningBadgeBorder
        StatusLevel.Critical -> AppTheme.colors.criticalBadgeBorder
    }
