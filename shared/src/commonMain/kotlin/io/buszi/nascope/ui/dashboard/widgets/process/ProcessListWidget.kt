package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_overflow_more
import io.buszi.nascope.resources.widget_processes_card_process_format
import io.buszi.nascope.resources.widget_processes_col_name
import io.buszi.nascope.resources.widget_processes_col_pid
import io.buszi.nascope.resources.widget_processes_col_user
import io.buszi.nascope.resources.widget_processes_empty
import io.buszi.nascope.resources.widget_processes_sort_cpu
import io.buszi.nascope.resources.widget_processes_sort_mem
import io.buszi.nascope.resources.widget_processes_status_critical
import io.buszi.nascope.resources.widget_processes_status_elevated
import io.buszi.nascope.resources.widget_processes_status_healthy
import io.buszi.nascope.resources.widget_processes_subtitle
import io.buszi.nascope.resources.widget_processes_subtitle_compact
import io.buszi.nascope.resources.widget_processes_title
import io.buszi.nascope.resources.widget_processes_title_compact
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.previewRowSplit
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.SelectSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.ToggleSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewState
import io.buszi.nascope.ui.dashboard.widgets.rowsThatFit
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProcessListWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ProcessListViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    ProcessListWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun ProcessListWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val statusLabel = when (viewState.overallStatus) {
        StatusLevel.Operational -> stringResource(Res.string.widget_processes_status_healthy)
        StatusLevel.Degraded -> stringResource(Res.string.widget_processes_status_elevated)
        StatusLevel.Critical -> stringResource(Res.string.widget_processes_status_critical)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 480.dp
        val isFixedHeight = constraints.hasBoundedHeight

        val subtitleText = if (isWide) {
            stringResource(
                Res.string.widget_processes_subtitle,
                viewState.runningProcessCount,
                viewState.totalProcessCount,
            )
        } else {
            stringResource(
                Res.string.widget_processes_subtitle_compact,
                viewState.runningProcessCount,
                viewState.totalProcessCount,
            )
        }

        val sortLabel = when (viewState.sortBy) {
            ProcessSortBy.Cpu -> stringResource(Res.string.widget_processes_sort_cpu)
            ProcessSortBy.Memory -> stringResource(Res.string.widget_processes_sort_mem)
        }

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_processes_title)
            } else {
                stringResource(Res.string.widget_processes_title_compact)
            },
            subtitle = subtitleText,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = {
                StatusChip(
                    text = statusLabel,
                    status = viewState.overallStatus,
                    pulsing = viewState.overallStatus == StatusLevel.Critical,
                )
            },
            headerAction = {
                Box(
                    modifier = Modifier
                        .clip(AppTheme.shapes.base)
                        .clickable(role = Role.Button) {
                            onViewAction(ToggleSortBy)
                        },
                ) {
                    TagChip(text = sortLabel)
                }
            },
        ) {
            if (viewState.processes.isEmpty()) {
                EmptyProcessesPlaceholder()
            } else {
                if (isFixedHeight) {
                    // Fixed-height brick slot: top-N preview with overflow count
                    ProcessPreviewTable(
                        viewState = viewState,
                        showDetails = isWide,
                        onViewAction = onViewAction,
                    )
                } else if (isWide) {
                    // Desktop / Wide Tablet: Tabular view
                    ProcessTableView(
                        viewState = viewState,
                        onViewAction = onViewAction,
                    )
                } else {
                    // Mobile / Compact: Responsive Swipe Cards
                    ProcessSwipeCardsView(
                        viewState = viewState,
                        onViewAction = onViewAction,
                    )
                }
            }
        }
    }
}

/**
 * Glanceable table used when the brick has a fixed height: renders as many rows as the slot
 * fits and summarises the rest as "+N MORE".
 * PID and user columns are only shown when [showDetails] is set (enough width).
 */
@Composable
private fun ProcessPreviewTable(
    viewState: ViewState,
    showDetails: Boolean,
    onViewAction: (ViewAction) -> Unit,
) {
    val rowSpacing = AppTheme.spacing.space2xs

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val split = previewRowSplit(
        totalItems = viewState.processes.size,
        capacity = rowsThatFit(
            availableHeight = maxHeight - PREVIEW_HEADER_HEIGHT - rowSpacing,
            rowHeight = PREVIEW_ROW_HEIGHT,
            spacing = rowSpacing,
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEADER_HEIGHT)
                .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = AppTheme.colors.baseBorder,
                    shape = AppTheme.shapes.base,
                )
                .padding(horizontal = AppTheme.spacing.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDetails) {
                Text(
                    text = stringResource(Res.string.widget_processes_col_pid),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    modifier = Modifier.width(60.dp),
                )
            }
            Text(
                text = stringResource(Res.string.widget_processes_col_name),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (showDetails) {
                Text(
                    text = stringResource(Res.string.widget_processes_col_user),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    modifier = Modifier.width(70.dp),
                )
            }
            Text(
                text = if (viewState.sortBy == ProcessSortBy.Cpu) "▼ CPU%" else "CPU%",
                style = AppTheme.typography.labelCode,
                color = if (viewState.sortBy == ProcessSortBy.Cpu) AppTheme.colors.telemetryA else AppTheme.colors.textMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .width(PREVIEW_METRIC_WIDTH)
                    .clip(AppTheme.shapes.base)
                    .clickable { onViewAction(SelectSortBy(ProcessSortBy.Cpu)) },
            )
            Text(
                text = if (viewState.sortBy == ProcessSortBy.Memory) "▼ MEM%" else "MEM%",
                style = AppTheme.typography.labelCode,
                color = if (viewState.sortBy == ProcessSortBy.Memory) AppTheme.colors.telemetryB else AppTheme.colors.textMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .width(PREVIEW_METRIC_WIDTH)
                    .clip(AppTheme.shapes.base)
                    .clickable { onViewAction(SelectSortBy(ProcessSortBy.Memory)) },
            )
        }

        viewState.processes.take(split.shown).forEach { process ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PREVIEW_ROW_HEIGHT)
                    .padding(horizontal = AppTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showDetails) {
                    Text(
                        text = process.pid.toString(),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                        maxLines = 1,
                        modifier = Modifier.width(60.dp),
                    )
                }
                Text(
                    text = process.name,
                    style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showDetails) {
                    Text(
                        text = process.user,
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(70.dp),
                    )
                }
                Text(
                    text = formatPercent(process.cpuPercent),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.telemetryA,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(PREVIEW_METRIC_WIDTH),
                )
                Text(
                    text = formatPercent(process.memoryPercent),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.telemetryB,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(PREVIEW_METRIC_WIDTH),
                )
            }
        }

        if (split.overflow > 0) {
            Text(
                text = stringResource(Res.string.widget_overflow_more, split.overflow),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier
                    .height(PREVIEW_ROW_HEIGHT)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = AppTheme.spacing.spaceSm),
            )
        }
    }
    }
}

private val PREVIEW_ROW_HEIGHT = 20.dp
private val PREVIEW_HEADER_HEIGHT = 24.dp
private val PREVIEW_METRIC_WIDTH = 56.dp

/**
 * Tabular layout for desktop and tablet screens.
 */
@Composable
private fun ProcessTableView(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.tableHeaderHeight)
                .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = AppTheme.colors.baseBorder,
                    shape = AppTheme.shapes.base,
                )
                .padding(horizontal = AppTheme.spacing.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.widget_processes_col_pid),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.width(60.dp),
            )

            Text(
                text = stringResource(Res.string.widget_processes_col_name),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = stringResource(Res.string.widget_processes_col_user),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.width(70.dp),
            )

            // Clickable CPU Sort Header
            Row(
                modifier = Modifier
                    .width(75.dp)
                    .clip(AppTheme.shapes.base)
                    .clickable { onViewAction(SelectSortBy(ProcessSortBy.Cpu)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = if (viewState.sortBy == ProcessSortBy.Cpu) "▼ CPU%" else "CPU%",
                    style = AppTheme.typography.labelCode,
                    color = if (viewState.sortBy == ProcessSortBy.Cpu) AppTheme.colors.telemetryA else AppTheme.colors.textMuted,
                )
            }

            Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))

            // Clickable MEM Sort Header
            Row(
                modifier = Modifier
                    .width(75.dp)
                    .clip(AppTheme.shapes.base)
                    .clickable { onViewAction(SelectSortBy(ProcessSortBy.Memory)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = if (viewState.sortBy == ProcessSortBy.Memory) "▼ MEM%" else "MEM%",
                    style = AppTheme.typography.labelCode,
                    color = if (viewState.sortBy == ProcessSortBy.Memory) AppTheme.colors.telemetryB else AppTheme.colors.textMuted,
                )
            }
        }

        // Table Rows
        viewState.processes.forEach { process ->
            ProcessTableRow(process = process)
        }
    }
}

@Composable
private fun ProcessTableRow(
    process: ProcessUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .padding(
                horizontal = AppTheme.spacing.spaceSm,
                vertical = AppTheme.spacing.spaceXs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = process.pid.toString(),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            modifier = Modifier.width(60.dp),
        )

        Text(
            text = process.name,
            style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = process.user,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(70.dp),
        )

        Text(
            text = formatPercent(process.cpuPercent),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.telemetryA,
            textAlign = TextAlign.End,
            modifier = Modifier.width(75.dp),
        )

        Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))

        Text(
            text = formatPercent(process.memoryPercent),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.telemetryB,
            textAlign = TextAlign.End,
            modifier = Modifier.width(75.dp),
        )
    }
}

/**
 * Mobile swipeable card presentation for compact widths.
 */
@Composable
private fun ProcessSwipeCardsView(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { viewState.processes.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // Sort Toggle Row on Mobile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.widget_processes_card_process_format,
                    pagerState.currentPage + 1,
                    viewState.processes.size,
                ),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
                SortChip(
                    text = "CPU",
                    isSelected = viewState.sortBy == ProcessSortBy.Cpu,
                    activeColor = AppTheme.colors.telemetryA,
                    onClick = { onViewAction(SelectSortBy(ProcessSortBy.Cpu)) },
                )
                SortChip(
                    text = "MEM",
                    isSelected = viewState.sortBy == ProcessSortBy.Memory,
                    activeColor = AppTheme.colors.telemetryB,
                    onClick = { onViewAction(SelectSortBy(ProcessSortBy.Memory)) },
                )
            }
        }

        // Swipeable Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            val process = viewState.processes[pageIndex]
            ProcessSwipeCard(process = process)
        }

        // Pager Navigation & Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                text = "◄ PREV",
                size = ButtonSize.Dense,
                enabled = pagerState.currentPage > 0,
                onClick = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
                repeat(viewState.processes.size.coerceAtMost(8)) { index ->
                    val isCurrent = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isCurrent) 16.dp else 6.dp,
                                height = 6.dp,
                            )
                            .clip(AppTheme.shapes.full)
                            .background(
                                if (isCurrent) AppTheme.colors.telemetryA else AppTheme.colors.baseBorder,
                            ),
                    )
                }
            }

            SecondaryButton(
                text = "NEXT ►",
                size = ButtonSize.Dense,
                enabled = pagerState.currentPage < viewState.processes.size - 1,
                onClick = {
                    if (pagerState.currentPage < viewState.processes.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SortChip(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppTheme.shapes.base)
            .background(
                if (isSelected) activeColor.copy(alpha = 0.15f) else AppTheme.colors.inputBackground,
                AppTheme.shapes.base,
            )
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (isSelected) activeColor else AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = AppTheme.spacing.spaceSm,
                vertical = AppTheme.spacing.space2xs,
            ),
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelCode,
            color = if (isSelected) activeColor else AppTheme.colors.textMuted,
        )
    }
}

@Composable
private fun ProcessSwipeCard(
    process: ProcessUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceMd),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            // Header Row: Name & PID Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = process.name,
                    style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                TagChip(text = "PID ${process.pid}")
            }

            // User & Memory RSS Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "USER: ${process.user}",
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )

                if (process.memoryRssBytes > 0L) {
                    Text(
                        text = "RSS: ${formatProcessMemory(process.memoryRssBytes)}",
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                }
            }

            // Dual Gauge Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                // CPU Metric Sub-card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppTheme.shapes.base)
                        .background(AppTheme.colors.canvasBackground, AppTheme.shapes.base)
                        .border(
                            width = AppTheme.dimensions.hairlineBorder,
                            color = AppTheme.colors.baseBorder,
                            shape = AppTheme.shapes.base,
                        )
                        .padding(AppTheme.spacing.spaceSm),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
                        Text(
                            text = "CPU LOAD",
                            style = AppTheme.typography.labelCode,
                            color = AppTheme.colors.textMuted,
                        )
                        MetricValue(
                            value = formatPercentNumber(process.cpuPercent),
                            unit = "%",
                            size = MetricSize.Medium,
                            valueColor = AppTheme.colors.telemetryA,
                        )
                        LinearGauge(
                            progress = (process.cpuPercent / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Memory Metric Sub-card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppTheme.shapes.base)
                        .background(AppTheme.colors.canvasBackground, AppTheme.shapes.base)
                        .border(
                            width = AppTheme.dimensions.hairlineBorder,
                            color = AppTheme.colors.baseBorder,
                            shape = AppTheme.shapes.base,
                        )
                        .padding(AppTheme.spacing.spaceSm),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
                        Text(
                            text = "MEMORY",
                            style = AppTheme.typography.labelCode,
                            color = AppTheme.colors.textMuted,
                        )
                        MetricValue(
                            value = formatPercentNumber(process.memoryPercent),
                            unit = "%",
                            size = MetricSize.Medium,
                            valueColor = AppTheme.colors.telemetryB,
                        )
                        LinearGauge(
                            progress = (process.memoryPercent / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProcessesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceLg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_processes_empty),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
        )
    }
}

internal fun formatPercent(value: Float): String {
    val rounded = ((value * 10).toLong() / 10.0).toString()
    return "$rounded%"
}

internal fun formatPercentNumber(value: Float): String {
    return ((value * 10).toLong() / 10.0).toString()
}

internal fun formatProcessMemory(bytes: Long): String {
    val absBytes = abs(bytes)
    return when {
        absBytes >= 1_000_000_000L -> {
            val gb = bytes / 1_000_000_000.0
            val formatted = ((gb * 10).toLong() / 10.0).toString()
            "$formatted GB"
        }
        absBytes >= 1_000_000L -> {
            val mb = bytes / 1_000_000.0
            val formatted = ((mb * 10).toLong() / 10.0).toString()
            "$formatted MB"
        }
        absBytes >= 1_000L -> {
            val kb = bytes / 1_000.0
            val formatted = ((kb * 10).toLong() / 10.0).toString()
            "$formatted KB"
        }
        else -> "$bytes B"
    }
}

@Preview
@Composable
private fun ProcessListWidgetPreview(
    @PreviewParameter(ProcessListPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
        ) {
            ProcessListWidget(viewState = viewState)
        }
    }
}
