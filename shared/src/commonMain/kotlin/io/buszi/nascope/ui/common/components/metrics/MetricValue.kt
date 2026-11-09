package io.buszi.nascope.ui.common.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Primary numerical figure for telemetric readouts.
 *
 * Formatted with monospace typography and tabular numerals (`tnum`), an optional unit label,
 * and an optional trend delta badge.
 *
 * @param value The primary numeric readout, e.g. `"842.6"`, `"42.8"`, `"128"`.
 * @param modifier Root layout modifier.
 * @param unit Optional measurement unit, e.g. `"MB/s"`, `"%"`, `"GB"`, `"ms"`.
 * @param trend Optional delta or rate-of-change text rendered via [TrendBadge], e.g. `"+1.2% / 5m"`.
 * @param trendColor Accent color for the trend badge. Defaults to [AppColors.operational].
 * @param size Visual scale preset ([MetricSize.Display] or [MetricSize.Medium]).
 * @param inlineTrend Whether to place the trend badge horizontally adjacent to the value rather than stacked below.
 * @param valueColor Color of the numerical text. Defaults to [AppColors.textPrimary].
 * @param unitColor Color of the unit text. Defaults to [AppColors.textMuted].
 */
@Composable
fun MetricValue(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    trend: String? = null,
    trendColor: Color = AppTheme.colors.operational,
    size: MetricSize = MetricSize.Display,
    inlineTrend: Boolean = false,
    valueColor: Color = AppTheme.colors.textPrimary,
    unitColor: Color = AppTheme.colors.textMuted,
) {
    MetricValue(
        value = value,
        modifier = modifier,
        unit = unit,
        size = size,
        inlineTrend = inlineTrend,
        valueColor = valueColor,
        unitColor = unitColor,
        trendBadge = trend?.let {
            {
                TrendBadge(
                    text = it,
                    color = trendColor,
                )
            }
        },
    )
}

/**
 * Overload of [MetricValue] supporting a semantic [StatusLevel] for the trend badge.
 */
@Composable
fun MetricValue(
    value: String,
    trend: String,
    trendStatus: StatusLevel,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: MetricSize = MetricSize.Display,
    inlineTrend: Boolean = false,
    valueColor: Color = AppTheme.colors.textPrimary,
    unitColor: Color = AppTheme.colors.textMuted,
) {
    MetricValue(
        value = value,
        modifier = modifier,
        unit = unit,
        size = size,
        inlineTrend = inlineTrend,
        valueColor = valueColor,
        unitColor = unitColor,
        trendBadge = {
            TrendBadge(
                text = trend,
                status = trendStatus,
            )
        },
    )
}

/**
 * Slot-based [MetricValue] for custom trend indicators or trailing accessories.
 *
 * @param value The primary numeric readout.
 * @param modifier Root layout modifier.
 * @param unit Optional measurement unit.
 * @param size Visual scale preset.
 * @param inlineTrend Whether to place [trendBadge] horizontally adjacent to the value.
 * @param valueColor Color of the numerical text.
 * @param unitColor Color of the unit text.
 * @param trendBadge Optional custom composable slot for the trend badge or indicator.
 */
@Composable
fun MetricValue(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: MetricSize = MetricSize.Display,
    inlineTrend: Boolean = false,
    valueColor: Color = AppTheme.colors.textPrimary,
    unitColor: Color = AppTheme.colors.textMuted,
    trendBadge: (@Composable () -> Unit)? = null,
) {
    val valueStyle = when (size) {
        MetricSize.Display -> AppTheme.typography.metricDisplay
        MetricSize.Medium -> AppTheme.typography.metricMd
    }
    val unitStyle = when (size) {
        MetricSize.Display -> AppTheme.typography.bodySm
        MetricSize.Medium -> AppTheme.typography.labelCode
    }
    val unitBottomPadding = when (size) {
        MetricSize.Display -> AppTheme.spacing.spaceXs
        MetricSize.Medium -> AppTheme.spacing.space2xs
    }

    val valueAndUnit = @Composable {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Text(
                text = value,
                style = valueStyle,
                color = valueColor,
                maxLines = 1,
            )
            if (!unit.isNullOrBlank()) {
                Text(
                    text = unit,
                    style = unitStyle,
                    color = unitColor,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = unitBottomPadding),
                )
            }
        }
    }

    if (inlineTrend && trendBadge != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            valueAndUnit()
            trendBadge()
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            valueAndUnit()
            if (trendBadge != null) {
                trendBadge()
            }
        }
    }
}

@Preview
@Composable
private fun MetricValuePreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            // Display size variants
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXl)) {
                MetricValue(
                    value = "842.6",
                    unit = "MB/s",
                    trend = "+12.4% / 5m",
                    trendColor = AppTheme.colors.operational,
                    size = MetricSize.Display,
                )
                MetricValue(
                    value = "42.8",
                    unit = "%",
                    trend = "-1.8% / 5m",
                    trendColor = AppTheme.colors.telemetryA,
                    size = MetricSize.Display,
                )
            }

            // Medium size variants (stacked and inline)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXl)) {
                MetricValue(
                    value = "1.42",
                    unit = "ms",
                    trend = "-0.15ms",
                    size = MetricSize.Medium,
                )
                MetricValue(
                    value = "94.2",
                    unit = "%",
                    trend = "+8.1% PEAK",
                    trendStatus = StatusLevel.Critical,
                    size = MetricSize.Medium,
                    inlineTrend = true,
                )
            }
        }
    }
}
