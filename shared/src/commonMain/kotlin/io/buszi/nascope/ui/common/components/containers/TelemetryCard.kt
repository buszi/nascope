package io.buszi.nascope.ui.common.components.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Layer 1 Surface Container (Card).
 * Standard substrate for telemetry widgets, metric groups, and modular dashboard panels.
 * Features a Zinc Surface fill (#12141A) with a 1px hairline border (#1E232F) and 8dp corner radius.
 */
@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.spaceLg),
    shape: Shape = AppTheme.shapes.lg,
    backgroundColor: Color = AppTheme.colors.subSurface,
    borderColor: Color = AppTheme.colors.baseBorder,
    borderWidth: Dp = AppTheme.dimensions.hairlineBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
    ) {
        if (title != null || headerAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = AppTheme.typography.labelUi,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.bodySm,
                            color = AppTheme.colors.textMuted,
                        )
                    }
                }

                if (headerAction != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                        content = headerAction,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
        }

        content()
    }
}

/**
 * Layer 1 Surface Container with custom header composable.
 */
@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.spaceLg),
    shape: Shape = AppTheme.shapes.lg,
    backgroundColor: Color = AppTheme.colors.subSurface,
    borderColor: Color = AppTheme.colors.baseBorder,
    borderWidth: Dp = AppTheme.dimensions.hairlineBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
    ) {
        if (header != null) {
            header()
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
        }
        content()
    }
}

@Preview
@Composable
private fun TelemetryCardPreview() {
    AppTheme {
        TelemetryCard(
            title = "NETWORK EGRESS",
            subtitle = "Active interface eth0",
            headerAction = {
                Text(
                    text = "ONLINE",
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.operational,
                )
            },
        ) {
            Text(
                text = "842.6 MB/s",
                style = AppTheme.typography.metricDisplay,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}
