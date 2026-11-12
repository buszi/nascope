package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_alerts_active
import io.buszi.nascope.resources.details_alerts_history
import io.buszi.nascope.resources.details_alerts_no_history
import io.buszi.nascope.resources.details_alerts_subtitle
import io.buszi.nascope.resources.details_alerts_title
import io.buszi.nascope.resources.details_sensors_stat_avg
import io.buszi.nascope.resources.details_sensors_stat_max
import io.buszi.nascope.resources.details_sensors_stat_min
import io.buszi.nascope.resources.widget_alerts_active_for
import io.buszi.nascope.resources.widget_alerts_all_clear
import io.buszi.nascope.resources.widget_alerts_lasted
import io.buszi.nascope.resources.widget_alerts_top_processes
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.model.AlertUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import io.buszi.nascope.ui.details.components.DetailsStatePlaceholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlertsDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<AlertsDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    AlertsDetailsScreen(viewState = viewState, onClose = onClose, modifier = modifier)
}

@Composable
fun AlertsDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alerts = viewState.alerts
    val isReady = viewState.widgetState.isReady
    val worst = alerts.worstStatus

    DetailsScaffold(
        title = stringResource(Res.string.details_alerts_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.Alerts),
        modifier = modifier,
        widgetState = viewState.widgetState,
        subtitle = if (isReady) {
            stringResource(Res.string.details_alerts_subtitle, alerts.activeCount, alerts.history.size)
        } else {
            null
        },
        statusTag = if (isReady) {
            {
                StatusChip(
                    text = if (worst == null) {
                        stringResource(Res.string.widget_alerts_all_clear)
                    } else {
                        alertStateLabel(alerts.active.first().state)
                    },
                    status = worst ?: StatusLevel.Operational,
                    pulsing = worst == StatusLevel.Critical,
                )
            }
        } else {
            null
        },
    ) {
        if (!isReady) {
            DetailsStatePlaceholder(viewState.widgetState)
        } else {
            if (alerts.active.isNotEmpty()) {
                DetailsSection(title = stringResource(Res.string.details_alerts_active)) {
                    AlertCards(alerts.active)
                }
            }
            DetailsSection(title = stringResource(Res.string.details_alerts_history)) {
                if (alerts.history.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.details_alerts_no_history),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                } else {
                    AlertCards(alerts.history)
                }
            }
        }
    }
}

@Composable
private fun AlertCards(alerts: List<AlertUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
        alerts.forEach { AlertCard(it) }
    }
}

@Composable
private fun AlertCard(alert: AlertUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(AppTheme.dimensions.hairlineBorder, AppTheme.colors.baseBorder, AppTheme.shapes.base)
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Text(
                text = alert.typeLabel,
                style = AppTheme.typography.codeBase,
                color = alert.status.color,
                modifier = Modifier.weight(1f),
            )
            alert.duration?.let {
                Text(
                    text = stringResource(
                        if (alert.isActive) Res.string.widget_alerts_active_for else Res.string.widget_alerts_lasted,
                        it,
                    ),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textSecondary,
                )
            }
            StatusChip(text = alertStateLabel(alert.state), status = alert.status)
        }
        alert.message?.let {
            Text(text = it, style = AppTheme.typography.bodySm, color = AppTheme.colors.textMuted)
        }
        if (alert.topProcesses.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.widget_alerts_top_processes, alert.topProcesses.joinToString(", ")),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        }
        if (alert.min != null || alert.avg != null || alert.max != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                alert.min?.let {
                    DetailsStat(stringResource(Res.string.details_sensors_stat_min), it, modifier = Modifier.weight(1f))
                }
                alert.avg?.let {
                    DetailsStat(stringResource(Res.string.details_sensors_stat_avg), it, modifier = Modifier.weight(1f))
                }
                alert.max?.let {
                    DetailsStat(stringResource(Res.string.details_sensors_stat_max), it, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview
@Composable
private fun AlertsDetailsScreenPreview(
    @PreviewParameter(AlertsDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        AlertsDetailsScreen(viewState = viewState, onClose = {})
    }
}
