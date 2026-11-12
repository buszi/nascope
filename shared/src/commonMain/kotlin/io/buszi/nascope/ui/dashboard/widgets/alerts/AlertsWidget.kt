package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_alerts_active_for
import io.buszi.nascope.resources.widget_alerts_all_clear
import io.buszi.nascope.resources.widget_alerts_state_careful
import io.buszi.nascope.resources.widget_alerts_state_critical
import io.buszi.nascope.resources.widget_alerts_state_unknown
import io.buszi.nascope.resources.widget_alerts_state_warning
import io.buszi.nascope.resources.widget_alerts_subtitle_active
import io.buszi.nascope.resources.widget_alerts_title
import io.buszi.nascope.resources.widget_overflow_more
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.model.AlertUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsViewModel.ViewState
import io.buszi.nascope.ui.dashboard.widgets.previewRowSplit
import io.buszi.nascope.ui.dashboard.widgets.rowsThatFit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlertsWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<AlertsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    AlertsWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun AlertsWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val isReady = viewState.widgetState.isReady
    val alerts = viewState.alerts
    val worst = alerts.worstStatus

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isFixedHeight = constraints.hasBoundedHeight

        DashboardWidget(
            title = stringResource(Res.string.widget_alerts_title),
            subtitle = if (isReady && alerts.activeCount > 0) {
                stringResource(Res.string.widget_alerts_subtitle_active, alerts.activeCount)
            } else {
                null
            },
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = if (isReady) {
                {
                    StatusChip(
                        text = if (worst == null) stringResource(Res.string.widget_alerts_all_clear) else alertStateLabel(alerts.active.first().state),
                        status = worst ?: StatusLevel.Operational,
                        pulsing = worst == StatusLevel.Critical,
                    )
                }
            } else {
                null
            },
        ) {
            if (alerts.active.isEmpty()) {
                AllClearPlaceholder()
            } else {
                AlertsPreviewList(alerts = alerts.active, isFixedHeight = isFixedHeight)
            }
        }
    }
}

@Composable
internal fun alertStateLabel(state: AlertState): String = stringResource(
    when (state) {
        AlertState.Careful -> Res.string.widget_alerts_state_careful
        AlertState.Warning -> Res.string.widget_alerts_state_warning
        AlertState.Critical -> Res.string.widget_alerts_state_critical
        AlertState.Unknown -> Res.string.widget_alerts_state_unknown
    },
)

@Composable
private fun AlertsPreviewList(
    alerts: List<AlertUi>,
    isFixedHeight: Boolean,
) {
    val rowSpacing = AppTheme.spacing.spaceXs

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val split = previewRowSplit(
            totalItems = alerts.size,
            capacity = if (isFixedHeight) {
                rowsThatFit(availableHeight = maxHeight, rowHeight = PreviewRowHeight, spacing = rowSpacing)
            } else {
                CompactCapacity
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
            alerts.take(split.shown).forEach { AlertPreviewRow(it) }
            if (split.overflow > 0) {
                Text(
                    text = stringResource(Res.string.widget_overflow_more, split.overflow),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviewRowHeight)
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }
}

/** One active alert: the metric, Glances' description and how long it has been running, in the state's colour. */
@Composable
private fun AlertPreviewRow(alert: AlertUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PreviewRowHeight),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = alert.typeLabel,
            style = AppTheme.typography.codeBase,
            color = alert.status.color,
            maxLines = 1,
        )
        Text(
            text = alert.message.orEmpty(),
            style = AppTheme.typography.bodySm,
            color = AppTheme.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        alert.duration?.let {
            Text(
                text = stringResource(Res.string.widget_alerts_active_for, it),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AllClearPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_alerts_all_clear),
            style = AppTheme.typography.bodySm,
            color = AppTheme.colors.textMuted,
        )
    }
}

private val PreviewRowHeight = 26.dp
private const val CompactCapacity = 4

@Preview
@Composable
private fun AlertsWidgetPreview(
    @PreviewParameter(AlertsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        AlertsWidget(viewState = viewState)
    }
}
