package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.Message

/**
 * Base scaffold container for all modular telemetry bricks.
 *
 * Implements a unified visual contract across all dashboard widgets:
 * - Layer 1 `subSurface` substrate with hairline border and `shapes.lg` radius.
 * - Standardized header displaying uppercase `labelUi` title, optional subtitle,
 *   optional status indicator / badge slot, and trailing action menu slot.
 * - Cohesive lifecycle state handling:
 *   - [WidgetState.Ready]: Renders [content].
 *   - [WidgetState.Loading]: Renders [loadingContent] (or default skeleton).
 *   - [WidgetState.Error]: Renders technical error readout with optional retry action.
 *   - [WidgetState.Offline]: Renders offline state indicator with optional reconnect action.
 *
 * @param title Section header label rendered in monospace-aligned label typography.
 * @param modifier Root layout modifier applied to the widget container.
 * @param subtitle Optional secondary descriptor text.
 * @param state Current lifecycle status of the widget.
 * @param statusTag Optional status chip / badge rendered alongside the title.
 * @param headerAction Optional trailing action slot in the header (e.g. settings icon or action button).
 * @param contentPadding Internal padding applied to the widget content area.
 * @param shape Outer corner radius shape, defaults to [AppTheme.shapes.lg].
 * @param backgroundColor Fill color, defaults to [AppTheme.colors.subSurface].
 * @param borderColor Hairline border color, defaults to [AppTheme.colors.baseBorder].
 * @param borderWidth Hairline border thickness, defaults to [AppTheme.dimensions.hairlineBorder].
 * @param loadingContent Custom skeleton slot to show during [WidgetState.Loading].
 * @param content Primary telemetry UI rendered when [state] is [WidgetState.Ready].
 */
@Composable
fun DashboardWidget(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    state: WidgetState = WidgetState.Ready,
    statusTag: (@Composable () -> Unit)? = null,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.spaceLg),
    shape: Shape = AppTheme.shapes.lg,
    backgroundColor: Color = AppTheme.colors.subSurface,
    borderColor: Color = AppTheme.colors.baseBorder,
    borderWidth: Dp = AppTheme.dimensions.hairlineBorder,
    loadingContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        // Bounded height means the grid enforces a fixed brick slot: fill it and clip the content.
        val hasBoundedHeight = constraints.hasBoundedHeight

        Column(
            modifier = Modifier
                .then(if (hasBoundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .clip(shape)
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
                .padding(contentPadding),
        ) {
        // Standardized Widget Header
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val isCompact = maxWidth < 480.dp
            val wrapHeaderAction = (subtitle != null && isCompact) || (subtitle == null && maxWidth < 320.dp && headerAction != null)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                if (wrapHeaderAction) {
                    // Line 1: Title and Status Tag with full horizontal breathing room
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    ) {
                        Text(
                            text = title,
                            style = AppTheme.typography.labelUi,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (statusTag != null) {
                            statusTag()
                        }
                    }

                    // Line 2: Subtitle (if present) on left, Header Actions on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = AppTheme.typography.bodySm,
                                color = AppTheme.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (headerAction != null) {
                            Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                                content = headerAction,
                            )
                        }
                    }
                } else {
                    // Wide layout (or single-line header with enough width)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                        ) {
                            Text(
                                text = title,
                                style = AppTheme.typography.labelUi,
                                color = AppTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (statusTag != null) {
                                statusTag()
                            }
                        }

                        if (headerAction != null) {
                            Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                                content = headerAction,
                            )
                        }
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.bodySm,
                            color = AppTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))

        // State-driven Content Rendering
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasBoundedHeight) Modifier.weight(1f) else Modifier)
                .clipToBounds(),
        ) {
            when (state) {
                is WidgetState.Ready -> {
                    content()
                }
                is WidgetState.Loading -> {
                    if (loadingContent != null) {
                        loadingContent()
                    } else {
                        DefaultWidgetSkeleton()
                    }
                }
                is WidgetState.Error -> {
                    WidgetErrorState(
                        message = state.message,
                        onRetry = state.onRetry,
                    )
                }
                is WidgetState.Offline -> {
                    WidgetOfflineState(
                        reason = state.reason,
                        onReconnect = state.onReconnect,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DefaultWidgetSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        WidgetSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(AppTheme.spacing.spaceLg),
        )
        WidgetSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.inputHeight),
        )
        WidgetSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(AppTheme.spacing.spaceSm),
        )
    }
}

@Composable
private fun WidgetErrorState(
    message: Message,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.criticalBadgeBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.criticalBadgeBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            StatusDot(color = AppTheme.colors.critical)
            Text(
                text = "TELEMETRY ERROR",
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.critical,
            )
        }
        Text(
            text = message.getText(),
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
            SecondaryButton(
                text = "Retry Telemetry",
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun WidgetOfflineState(
    reason: Message,
    onReconnect: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.warningBadgeBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.warningBadgeBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            StatusDot(color = AppTheme.colors.degraded)
            Text(
                text = "SUBSYSTEM OFFLINE",
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.degraded,
            )
        }
        Text(
            text = reason.getText(),
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
        )
        if (onReconnect != null) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
            SecondaryButton(
                text = "Reconnect Daemon",
                onClick = onReconnect,
            )
        }
    }
}

@Preview
@Composable
private fun DashboardWidgetPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
        ) {
            DashboardWidget(
                title = "NETWORK EGRESS",
                subtitle = "Active interface eth0",
                statusTag = {
                    StatusChip(
                        text = "10 GbE",
                        status = StatusLevel.Operational,
                    )
                },
                headerAction = {
                    TagChip(text = "MUT 1500")
                },
            ) {
                Text(
                    text = "842.6 MB/s",
                    style = AppTheme.typography.metricDisplay,
                    color = AppTheme.colors.textPrimary,
                )
            }

            DashboardWidget(
                title = "CPU PIPELINE",
                state = WidgetState.Loading,
            ) {
                // Not rendered in Loading state
            }

            DashboardWidget(
                title = "STORAGE POOL",
                state = WidgetState.Error(
                    message = "Connection timeout to host daemon at 192.168.1.120:61208",
                    onRetry = {},
                ),
            ) {
                // Not rendered in Error state
            }
        }
    }
}
