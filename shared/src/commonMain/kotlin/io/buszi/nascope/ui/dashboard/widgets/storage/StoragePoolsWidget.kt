package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_storage_io_rate_format
import io.buszi.nascope.resources.widget_storage_io_read
import io.buszi.nascope.resources.widget_storage_io_write
import io.buszi.nascope.resources.widget_storage_mounts_summary
import io.buszi.nascope.resources.widget_storage_mounts_summary_compact
import io.buszi.nascope.resources.widget_storage_no_mounts
import io.buszi.nascope.resources.widget_storage_page_indicator
import io.buszi.nascope.resources.widget_storage_pools_label
import io.buszi.nascope.resources.widget_storage_pools_label_compact
import io.buszi.nascope.resources.widget_storage_status_critical
import io.buszi.nascope.resources.widget_storage_status_degraded
import io.buszi.nascope.resources.widget_storage_status_healthy
import io.buszi.nascope.resources.widget_storage_throughput_label
import io.buszi.nascope.resources.widget_storage_title
import io.buszi.nascope.resources.widget_storage_title_compact
import io.buszi.nascope.resources.widget_storage_total_capacity
import io.buszi.nascope.resources.widget_storage_used_of_total
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StoragePoolsWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<StoragePoolsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    StoragePoolsWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun StoragePoolsWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val statusLabel = when (viewState.overallStatus) {
        StatusLevel.Critical -> stringResource(Res.string.widget_storage_status_critical)
        StatusLevel.Degraded -> stringResource(Res.string.widget_storage_status_degraded)
        StatusLevel.Operational -> stringResource(Res.string.widget_storage_status_healthy)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val isWide = containerWidth >= 330.dp

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_storage_title)
            } else {
                stringResource(Res.string.widget_storage_title_compact)
            },
            subtitle = if (viewState.widgetState.isReady && viewState.mounts.isNotEmpty()) {
                stringResource(Res.string.widget_storage_total_capacity, formatCapacity(viewState.totalCapacityBytes))
            } else null,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = if (viewState.widgetState.isReady) {
                {
                    if (isWide) {
                        StatusChip(
                            text = statusLabel,
                            status = viewState.overallStatus,
                        )
                    } else {
                        StatusDot(
                            status = viewState.overallStatus,
                            pulsing = viewState.overallStatus == StatusLevel.Critical,
                        )
                    }
                }
            } else null,
            headerAction = if (viewState.widgetState.isReady && viewState.mounts.isNotEmpty()) {
                {
                    TagChip(
                        text = if (isWide) {
                            stringResource(Res.string.widget_storage_mounts_summary, viewState.mounts.size)
                        } else {
                            stringResource(Res.string.widget_storage_mounts_summary_compact, viewState.mounts.size)
                        },
                    )
                }
            } else null,
        ) {
            if (viewState.widgetState.isReady) {
                if (isWide) {
                    WideStoragePoolsContent(
                        viewState = viewState,
                        maxWidth = containerWidth,
                        onViewAction = onViewAction,
                    )
                } else {
                    CompactStoragePoolsContent(
                        viewState = viewState,
                        onViewAction = onViewAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideStoragePoolsContent(
    viewState: ViewState,
    maxWidth: Dp,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Disk I/O Throughput Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Text(
                text = stringResource(Res.string.widget_storage_throughput_label),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DiskIoCell(
                    label = stringResource(Res.string.widget_storage_io_read),
                    bytesPerSec = viewState.readRateBytesPerSec,
                    indicatorColor = AppTheme.colors.telemetryB,
                    modifier = Modifier.weight(1f),
                )
                DiskIoCell(
                    label = stringResource(Res.string.widget_storage_io_write),
                    bytesPerSec = viewState.writeRateBytesPerSec,
                    indicatorColor = AppTheme.colors.telemetryA,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        // Mount Points Section
        MountPointsListSection(
            viewState = viewState,
            isWide = true,
            onViewAction = onViewAction,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactStoragePoolsContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // Compact Disk I/O Row: read and write stay intact and wrap as units when the brick is narrow
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = AppTheme.colors.baseBorder,
                    shape = AppTheme.shapes.base,
                )
                .padding(horizontal = AppTheme.spacing.spaceSm, vertical = AppTheme.spacing.spaceXs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.spacing.spaceXs)
                        .background(AppTheme.colors.telemetryB, AppTheme.shapes.full),
                )
                Text(
                    text = stringResource(Res.string.widget_storage_io_read),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
                Text(
                    text = stringResource(Res.string.widget_storage_io_rate_format, formatMbRate(viewState.readRateBytesPerSec)),
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.spacing.spaceXs)
                        .background(AppTheme.colors.telemetryA, AppTheme.shapes.full),
                )
                Text(
                    text = stringResource(Res.string.widget_storage_io_write),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
                Text(
                    text = stringResource(Res.string.widget_storage_io_rate_format, formatMbRate(viewState.writeRateBytesPerSec)),
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        // Compact Mount Points Section
        MountPointsListSection(
            viewState = viewState,
            isWide = false,
            onViewAction = onViewAction,
        )
    }
}

@Composable
private fun DiskIoCell(
    label: String,
    bytesPerSec: Long,
    indicatorColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.spaceXs)
                    .background(indicatorColor, AppTheme.shapes.full),
            )
        }

        MetricValue(
            value = formatMbRate(bytesPerSec),
            unit = "MB/s",
            size = MetricSize.Medium,
        )
    }
}

@Composable
private fun MountPointsListSection(
    viewState: ViewState,
    isWide: Boolean,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        // Section Header with Pagination Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isWide) {
                    stringResource(Res.string.widget_storage_pools_label)
                } else {
                    stringResource(Res.string.widget_storage_pools_label_compact)
                },
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )

            if (viewState.totalPages > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PaginationArrowButton(
                        symbol = "‹",
                        enabled = viewState.currentPage > 0,
                        onClick = { onViewAction(ViewAction.OnPrevPage) },
                    )

                    Text(
                        text = stringResource(Res.string.widget_storage_page_indicator, viewState.currentPage + 1, viewState.totalPages),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textSecondary,
                    )

                    PaginationArrowButton(
                        symbol = "›",
                        enabled = viewState.currentPage < viewState.totalPages - 1,
                        onClick = { onViewAction(ViewAction.OnNextPage) },
                    )
                }
            }
        }

        if (viewState.displayedMounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.spaceLg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.widget_storage_no_mounts),
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textDisabled,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                viewState.displayedMounts.forEach { mount ->
                    MountPointCard(
                        mount = mount,
                        isWide = isWide,
                    )
                }
            }
        }
    }
}

@Composable
private fun MountPointCard(
    mount: StorageMountUi,
    isWide: Boolean,
    modifier: Modifier = Modifier,
) {
    val ratio = mount.usageRatio
    val percentage = (ratio * 100).toInt()
    val mountStatus = mount.status

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        // Mount Path & Device Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mount.mountPoint,
                    style = if (isWide) AppTheme.typography.headlineSm else AppTheme.typography.codeBase,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (mount.fsType.isNotBlank()) {
                    TagChip(text = mount.fsType)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mount.deviceName.isNotBlank()) {
                    Text(
                        text = mount.deviceName,
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                StatusDot(
                    status = mountStatus,
                    pulsing = mountStatus == StatusLevel.Critical,
                )
            }
        }

        // Capacity Usage Values & Percentage Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(
                    Res.string.widget_storage_used_of_total,
                    formatCapacity(mount.usedBytes),
                    formatCapacity(mount.totalBytes),
                ),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
            )

            Text(
                text = stringResource(Res.string.percentage_format, percentage),
                style = AppTheme.typography.labelCode,
                color = when (mountStatus) {
                    StatusLevel.Critical -> AppTheme.colors.critical
                    StatusLevel.Degraded -> AppTheme.colors.degraded
                    StatusLevel.Operational -> AppTheme.colors.textSecondary
                },
            )
        }

        // Capacity Gauge
        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            warningThreshold = StorageMountUi.WarningThreshold,
            criticalThreshold = StorageMountUi.CriticalThreshold,
            normalColor = AppTheme.colors.telemetryA,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun PaginationArrowButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (enabled) AppTheme.colors.inputBackground else androidx.compose.ui.graphics.Color.Transparent,
                shape = AppTheme.shapes.sm,
            )
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (enabled) AppTheme.colors.baseBorder else AppTheme.colors.subtleDivider,
                shape = AppTheme.shapes.sm,
            )
            .clip(AppTheme.shapes.sm)
            .optionalModifier(enabled) { clickable(onClick = onClick) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = AppTheme.typography.codeBase,
            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textDisabled,
        )
    }
}

internal fun formatCapacity(bytes: Long): String {
    if (bytes <= 0L) return "0 GB"
    val terabytes = 1_000_000_000_000L
    val gigabytes = 1_000_000_000L
    val megabytes = 1_000_000L

    return when {
        bytes >= terabytes -> {
            val rounded = (bytes * 10 / terabytes).toInt()
            val whole = rounded / 10
            val fraction = rounded % 10
            "$whole.$fraction TB"
        }
        bytes >= gigabytes -> {
            val rounded = (bytes * 10 / gigabytes).toInt()
            val whole = rounded / 10
            val fraction = rounded % 10
            "$whole.$fraction GB"
        }
        else -> {
            val mb = bytes / megabytes
            "$mb MB"
        }
    }
}

internal fun formatMbRate(bytesPerSec: Long): String {
    val mbPerSec = bytesPerSec / 1_000_000.0f
    val rounded = (mbPerSec * 10).toInt()
    val whole = rounded / 10
    val fraction = kotlin.math.abs(rounded % 10)
    return "$whole.$fraction"
}

@Preview
@Composable
private fun StoragePoolsWidgetPreview(
    @PreviewParameter(StoragePoolsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        StoragePoolsWidget(
            viewState = viewState,
            onViewAction = {},
        )
    }
}
