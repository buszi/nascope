package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_storage_distribution
import io.buszi.nascope.resources.details_storage_field_device
import io.buszi.nascope.resources.details_storage_field_filesystem
import io.buszi.nascope.resources.details_storage_field_free
import io.buszi.nascope.resources.details_storage_field_inodes
import io.buszi.nascope.resources.details_storage_field_options
import io.buszi.nascope.resources.details_storage_free_format
import io.buszi.nascope.resources.details_storage_io
import io.buszi.nascope.resources.details_storage_mounts
import io.buszi.nascope.resources.details_storage_no_mounts
import io.buszi.nascope.resources.details_storage_overview
import io.buszi.nascope.resources.details_storage_sort_name
import io.buszi.nascope.resources.details_storage_sort_size
import io.buszi.nascope.resources.details_storage_sort_usage
import io.buszi.nascope.resources.details_storage_subtitle
import io.buszi.nascope.resources.details_storage_title
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_storage_io_rate_format
import io.buszi.nascope.resources.widget_storage_io_read
import io.buszi.nascope.resources.widget_storage_io_write
import io.buszi.nascope.resources.widget_storage_status_critical
import io.buszi.nascope.resources.widget_storage_status_degraded
import io.buszi.nascope.resources.widget_storage_status_healthy
import io.buszi.nascope.resources.widget_storage_used_of_total
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.inputs.SelectableChip
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsKeyValueRow
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StorageDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<StorageDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    StorageDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun StorageDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_storage_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.StoragePools),
        modifier = modifier,
        subtitle = if (viewState.widgetState.isReady) {
            stringResource(
                Res.string.details_storage_subtitle,
                viewState.mounts.size,
                formatCapacity(viewState.totalCapacityBytes),
            )
        } else {
            null
        },
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.overallStatus) {
                        StatusLevel.Critical -> stringResource(Res.string.widget_storage_status_critical)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_storage_status_degraded)
                        StatusLevel.Operational -> stringResource(Res.string.widget_storage_status_healthy)
                    },
                    status = viewState.overallStatus,
                )
            }
        } else {
            null
        },
    ) {
        if (!viewState.widgetState.isReady) {
            DetailsLoadingPlaceholder()
        } else if (viewState.mounts.isEmpty()) {
            Text(
                text = stringResource(Res.string.details_storage_no_mounts),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        } else {
            OverviewSection(viewState = viewState)
            DistributionSection(viewState = viewState)
            DiskIoSection(viewState = viewState)
            MountsSection(viewState = viewState, onViewAction = onViewAction)
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_storage_overview),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            MetricValue(
                value = (viewState.totalUsageRatio * 100).toInt().toString(),
                unit = "%",
                size = MetricSize.Display,
            )
            Text(
                text = stringResource(
                    Res.string.widget_storage_used_of_total,
                    formatCapacity(viewState.totalUsedBytes),
                    formatCapacity(viewState.totalCapacityBytes),
                ),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
            )
        }
        LinearGauge(
            progress = viewState.totalUsageRatio,
            modifier = Modifier.padding(vertical = AppTheme.spacing.spaceSm),
            warningThreshold = StorageMountUi.WarningThreshold,
            criticalThreshold = StorageMountUi.CriticalThreshold,
            normalColor = AppTheme.colors.telemetryA,
        )
        Text(
            text = stringResource(Res.string.details_storage_free_format, formatCapacity(viewState.totalFreeBytes)),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
        )
    }
}

@Composable
private fun distributionColors(): List<Color> = listOf(
    AppTheme.colors.telemetryA,
    AppTheme.colors.telemetryB,
    AppTheme.colors.operational,
    AppTheme.colors.degraded,
)

@Composable
private fun DistributionSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    val colors = distributionColors()
    val mounts = viewState.mounts.filter { it.usedBytes > 0L }
    val totalUsed = viewState.totalUsedBytes

    DetailsSection(
        title = stringResource(Res.string.details_storage_distribution),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.spacing.spaceMd)
                .clip(AppTheme.shapes.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.hairlineBorder),
        ) {
            mounts.forEachIndexed { index, mount ->
                Box(
                    modifier = Modifier
                        .weight(mount.usedBytes.toFloat())
                        .height(AppTheme.spacing.spaceMd)
                        .background(colors[index % colors.size]),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = AppTheme.spacing.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            mounts.forEachIndexed { index, mount ->
                val share = if (totalUsed > 0L) (mount.usedBytes * 100 / totalUsed).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppTheme.spacing.spaceXs * 2)
                            .background(colors[index % colors.size], AppTheme.shapes.full),
                    )
                    Text(
                        text = mount.mountPoint,
                        style = AppTheme.typography.codeBase,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatCapacity(mount.usedBytes),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = stringResource(Res.string.percentage_format, share),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiskIoSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_storage_io),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd)) {
            IoChannel(
                label = stringResource(Res.string.widget_storage_io_read),
                bytesPerSec = viewState.readRateBytesPerSec,
                history = viewState.readHistory,
                color = AppTheme.colors.telemetryB,
            )
            IoChannel(
                label = stringResource(Res.string.widget_storage_io_write),
                bytesPerSec = viewState.writeRateBytesPerSec,
                history = viewState.writeHistory,
                color = AppTheme.colors.telemetryA,
            )
        }
    }
}

@Composable
private fun IoChannel(
    label: String,
    bytesPerSec: Long,
    history: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.spaceXs)
                    .background(color, AppTheme.shapes.full),
            )
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.widget_storage_io_rate_format, formatMbRate(bytesPerSec)),
                style = AppTheme.typography.codeBase,
                color = AppTheme.colors.textPrimary,
            )
        }
        if (history.size >= 2) {
            MiniSparkline(
                data = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.sparklineHeight),
                lineColor = color,
                minValue = 0f,
            )
        }
    }
}

@Composable
private fun MountsSection(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_storage_mounts),
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
            SortOrder.entries.forEach { order ->
                SelectableChip(
                    text = order.label(),
                    selected = viewState.sortOrder == order,
                    onClick = { onViewAction(ViewAction.OnSelectSort(order)) },
                    modifier = Modifier.testTag(TestTags.storageSort(order.name)),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = AppTheme.spacing.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            viewState.sortedMounts.forEach { mount ->
                MountDetailsCard(
                    mount = mount,
                    expanded = viewState.expandedMountPoint == mount.mountPoint,
                    onToggle = { onViewAction(ViewAction.OnToggleMount(mount.mountPoint)) },
                )
            }
        }
    }
}

@Composable
private fun SortOrder.label(): String = when (this) {
    SortOrder.Usage -> stringResource(Res.string.details_storage_sort_usage)
    SortOrder.Name -> stringResource(Res.string.details_storage_sort_name)
    SortOrder.Size -> stringResource(Res.string.details_storage_sort_size)
}

@Composable
private fun MountDetailsCard(
    mount: StorageMountUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = mount.usageRatio
    val status = mount.status
    val shape = AppTheme.shapes.base

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.inputBackground, shape)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (expanded) AppTheme.colors.focusHighlight else AppTheme.colors.baseBorder,
                shape = shape,
            )
            .testTag(TestTags.storageMount(mount.mountPoint))
            .clickable(onClick = onToggle)
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                Text(
                    text = mount.mountPoint,
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (mount.fsType.isNotBlank()) {
                    TagChip(text = mount.fsType)
                }
            }
            StatusDot(
                status = status,
                pulsing = status == StatusLevel.Critical,
            )
            Text(
                text = stringResource(Res.string.percentage_format, (ratio * 100).toInt()),
                style = AppTheme.typography.labelCode,
                color = when (status) {
                    StatusLevel.Critical -> AppTheme.colors.critical
                    StatusLevel.Degraded -> AppTheme.colors.degraded
                    StatusLevel.Operational -> AppTheme.colors.textSecondary
                },
            )
        }

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            warningThreshold = StorageMountUi.WarningThreshold,
            criticalThreshold = StorageMountUi.CriticalThreshold,
            normalColor = AppTheme.colors.telemetryA,
        )

        Text(
            text = stringResource(
                Res.string.widget_storage_used_of_total,
                formatCapacity(mount.usedBytes),
                formatCapacity(mount.totalBytes),
            ),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textSecondary,
        )

        if (expanded) {
            Column(
                modifier = Modifier.padding(top = AppTheme.spacing.spaceXs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_storage_field_device),
                    value = mount.deviceName,
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_storage_field_filesystem),
                    value = mount.fsType,
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_storage_field_free),
                    value = formatCapacity(mount.freeBytes),
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_storage_field_options),
                    value = mount.mountOptions,
                )
                LinearGauge(
                    progress = mount.inodeUsagePercent / 100f,
                    label = stringResource(Res.string.details_storage_field_inodes),
                    valueText = stringResource(Res.string.percentage_format, mount.inodeUsagePercent.toInt()),
                    barHeight = AppTheme.spacing.space2xs,
                    normalColor = AppTheme.colors.telemetryB,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StorageDetailsScreenPreview(
    @PreviewParameter(StorageDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        StorageDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
