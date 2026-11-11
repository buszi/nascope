package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_processes_clear_search
import io.buszi.nascope.resources.details_processes_field_rss
import io.buszi.nascope.resources.details_processes_field_state
import io.buszi.nascope.resources.details_processes_field_user
import io.buszi.nascope.resources.details_processes_filter_all
import io.buszi.nascope.resources.details_processes_filter_format
import io.buszi.nascope.resources.details_processes_no_match
import io.buszi.nascope.resources.details_processes_overview
import io.buszi.nascope.resources.details_processes_results
import io.buszi.nascope.resources.details_processes_search_placeholder
import io.buszi.nascope.resources.details_processes_stat_cpu_sum
import io.buszi.nascope.resources.details_processes_stat_mem_sum
import io.buszi.nascope.resources.details_processes_stat_running
import io.buszi.nascope.resources.details_processes_stat_total
import io.buszi.nascope.resources.details_processes_subtitle
import io.buszi.nascope.resources.details_processes_table
import io.buszi.nascope.resources.details_processes_title
import io.buszi.nascope.resources.widget_processes_col_cpu
import io.buszi.nascope.resources.widget_processes_col_mem
import io.buszi.nascope.resources.widget_processes_col_name
import io.buszi.nascope.resources.widget_processes_col_pid
import io.buszi.nascope.resources.widget_processes_empty
import io.buszi.nascope.resources.widget_processes_status_critical
import io.buszi.nascope.resources.widget_processes_status_elevated
import io.buszi.nascope.resources.widget_processes_status_healthy
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.inputs.SelectableChip
import io.buszi.nascope.ui.common.components.inputs.TelemetryTextField
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.SortField
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsKeyValueRow
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsPager
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PidColumnWidth = 48.dp
private val LoadColumnWidth = 44.dp

@Composable
fun ProcessDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ProcessDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    ProcessDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun ProcessDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_processes_title),
        onClose = onClose,
        modifier = modifier,
        subtitle = if (viewState.widgetState.isReady) {
            stringResource(
                Res.string.details_processes_subtitle,
                viewState.processes.size,
                viewState.totalProcessCount,
            )
        } else {
            null
        },
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.overallStatus) {
                        StatusLevel.Operational -> stringResource(Res.string.widget_processes_status_healthy)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_processes_status_elevated)
                        StatusLevel.Critical -> stringResource(Res.string.widget_processes_status_critical)
                    },
                    status = viewState.overallStatus,
                    pulsing = viewState.overallStatus == StatusLevel.Critical,
                )
            }
        } else {
            null
        },
    ) {
        if (!viewState.widgetState.isReady) {
            DetailsLoadingPlaceholder()
        } else if (viewState.processes.isEmpty()) {
            Text(
                text = stringResource(Res.string.widget_processes_empty),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        } else {
            OverviewSection(viewState = viewState)
            ProcessTableSection(viewState = viewState, onViewAction = onViewAction)
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_processes_overview),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DetailsStat(
                    label = stringResource(Res.string.details_processes_stat_total),
                    value = viewState.totalProcessCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = stringResource(Res.string.details_processes_stat_running),
                    value = viewState.runningProcessCount.toString(),
                    valueColor = AppTheme.colors.operational,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DetailsStat(
                    label = stringResource(Res.string.details_processes_stat_cpu_sum),
                    value = formatPercent(viewState.totalCpuPercent),
                    valueColor = AppTheme.colors.telemetryA,
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = stringResource(Res.string.details_processes_stat_mem_sum),
                    value = formatPercent(viewState.totalMemoryPercent),
                    valueColor = AppTheme.colors.telemetryB,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessTableSection(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_processes_table),
        modifier = modifier,
        headerAction = {
            Text(
                text = stringResource(Res.string.details_processes_results, viewState.results.size),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            TelemetryTextField(
                value = viewState.query,
                onValueChange = { onViewAction(ViewAction.OnQueryChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.details_processes_search_placeholder),
                trailingIcon = if (viewState.query.isNotEmpty()) {
                    {
                        Text(
                            text = "×",
                            style = AppTheme.typography.codeBase,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier
                                .clip(AppTheme.shapes.sm)
                                .clickable(
                                    onClickLabel = stringResource(Res.string.details_processes_clear_search),
                                    role = Role.Button,
                                ) { onViewAction(ViewAction.OnQueryChange("")) }
                                .padding(horizontal = AppTheme.spacing.spaceXs),
                        )
                    }
                } else {
                    null
                },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                SelectableChip(
                    text = stringResource(
                        Res.string.details_processes_filter_format,
                        stringResource(Res.string.details_processes_filter_all),
                        viewState.processes.size,
                    ),
                    selected = viewState.stateFilter == null,
                    onClick = { onViewAction(ViewAction.OnSelectStateFilter(null)) },
                )
                viewState.availableStates.forEach { state ->
                    SelectableChip(
                        text = stringResource(
                            Res.string.details_processes_filter_format,
                            state.label(),
                            viewState.countOf(state),
                        ),
                        selected = viewState.stateFilter == state,
                        onClick = { onViewAction(ViewAction.OnSelectStateFilter(state)) },
                    )
                }
            }

            TableHeader(viewState = viewState, onViewAction = onViewAction)

            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
                if (viewState.results.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.details_processes_no_match),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                }
                viewState.pageProcesses.forEach { process ->
                    ProcessRow(
                        process = process,
                        expanded = viewState.expandedPid == process.pid,
                        onToggle = { onViewAction(ViewAction.OnToggleProcess(process.pid)) },
                    )
                }
            }

            if (viewState.pageCount > 1) {
                DetailsPager(
                    page = viewState.page,
                    pageCount = viewState.pageCount,
                    onPrevious = { onViewAction(ViewAction.OnPrevPage) },
                    onNext = { onViewAction(ViewAction.OnNextPage) },
                )
            }
        }
    }
}

@Composable
private fun TableHeader(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell(
            text = stringResource(Res.string.widget_processes_col_pid),
            field = SortField.Pid,
            viewState = viewState,
            onViewAction = onViewAction,
            modifier = Modifier.width(PidColumnWidth),
        )
        HeaderCell(
            text = stringResource(Res.string.widget_processes_col_name),
            field = SortField.Name,
            viewState = viewState,
            onViewAction = onViewAction,
            modifier = Modifier.weight(1f),
        )
        HeaderCell(
            text = stringResource(Res.string.widget_processes_col_cpu),
            field = SortField.Cpu,
            viewState = viewState,
            onViewAction = onViewAction,
            modifier = Modifier.width(LoadColumnWidth),
            textAlign = TextAlign.End,
        )
        HeaderCell(
            text = stringResource(Res.string.widget_processes_col_mem),
            field = SortField.Memory,
            viewState = viewState,
            onViewAction = onViewAction,
            modifier = Modifier.width(LoadColumnWidth),
            textAlign = TextAlign.End,
        )
        Box(modifier = Modifier.width(AppTheme.spacing.spaceLg))
    }
}

@Composable
private fun HeaderCell(
    text: String,
    field: SortField,
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val selected = viewState.sortField == field
    val arrow = if (viewState.sortDescending) "▼" else "▲"

    Text(
        text = if (selected) "$arrow$text" else text,
        style = AppTheme.typography.labelCode,
        color = if (selected) AppTheme.colors.telemetryA else AppTheme.colors.textMuted,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(AppTheme.shapes.sm)
            .clickable(role = Role.Button) { onViewAction(ViewAction.OnSelectSort(field)) }
            .padding(vertical = AppTheme.spacing.spaceXs),
    )
}

@Composable
private fun ProcessRow(
    process: ProcessUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.sm
    val state = process.state

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
            .clickable(onClick = onToggle)
            .padding(
                horizontal = AppTheme.spacing.spaceSm,
                vertical = AppTheme.spacing.spaceXs,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = process.pid.toString(),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                modifier = Modifier.width(PidColumnWidth),
            )
            Text(
                text = process.name,
                style = AppTheme.typography.codeBase,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPercentNumber(process.cpuPercent),
                style = AppTheme.typography.labelCode,
                color = if (process.cpuPercent > 0f) AppTheme.colors.telemetryA else AppTheme.colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(LoadColumnWidth),
            )
            Text(
                text = formatPercentNumber(process.memoryPercent),
                style = AppTheme.typography.labelCode,
                color = if (process.memoryPercent > 0f) AppTheme.colors.telemetryB else AppTheme.colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(LoadColumnWidth),
            )
            Box(
                modifier = Modifier.width(AppTheme.spacing.spaceLg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimensions.statusDotSize)
                        .background(state.accentColor(), AppTheme.shapes.full),
                )
            }
        }

        if (expanded) {
            ProcessDetails(process = process)
        }
    }
}

@Composable
private fun ProcessDetails(
    process: ProcessUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        DetailsKeyValueRow(
            label = stringResource(Res.string.details_processes_field_user),
            value = process.user,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.details_processes_field_state),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            TagChip(text = process.state.label(), dotColor = process.state.accentColor())
        }
        DetailsKeyValueRow(
            label = stringResource(Res.string.details_processes_field_rss),
            value = formatProcessMemory(process.memoryRssBytes),
        )
        LoadGauge(
            label = stringResource(Res.string.widget_processes_col_cpu),
            percent = process.cpuPercent,
            color = AppTheme.colors.telemetryA,
        )
        LoadGauge(
            label = stringResource(Res.string.widget_processes_col_mem),
            percent = process.memoryPercent,
            color = AppTheme.colors.telemetryB,
        )
    }
}

@Composable
private fun LoadGauge(
    label: String,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    LinearGauge(
        progress = percent / 100f,
        modifier = modifier,
        label = label,
        valueText = formatPercent(percent),
        barHeight = AppTheme.spacing.space2xs,
        warningThreshold = ProcessUi.WarningLoadPercent / 100f,
        criticalThreshold = ProcessUi.CriticalLoadPercent / 100f,
        normalColor = color,
    )
}

@Preview
@Composable
private fun ProcessDetailsScreenPreview(
    @PreviewParameter(ProcessDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        ProcessDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
