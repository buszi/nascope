package io.buszi.nascope.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.screen_settings_header
import io.buszi.nascope.resources.settings_refresh_interval_helper
import io.buszi.nascope.resources.settings_refresh_interval_title
import io.buszi.nascope.ui.common.components.containers.CenteredPanel
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.components.inputs.SelectableChip
import io.buszi.nascope.ui.common.model.RefreshIntervalUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.settings.SettingsViewModel.ViewAction
import io.buszi.nascope.ui.settings.SettingsViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SettingsContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun SettingsContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
) {
    CenteredPanel(
        modifier = Modifier
            .background(AppTheme.colors.canvasBackground)
            .testTag(TestTags.SettingsScreen),
        verticalAlignment = Alignment.Top,
    ) {
        TelemetryCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.screen_settings_header),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                Text(
                    text = stringResource(Res.string.settings_refresh_interval_title),
                    style = AppTheme.typography.headlineLg,
                    color = AppTheme.colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
                    RefreshIntervalUi.entries.forEach { interval ->
                        SelectableChip(
                            text = interval.label,
                            selected = interval == viewState.refreshInterval,
                            onClick = { onViewAction(ViewAction.OnSelectRefreshInterval(interval)) },
                            modifier = Modifier
                                .minimumTouchTarget()
                                .testTag(TestTags.settingsRefreshInterval(interval.name)),
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.settings_refresh_interval_helper),
                    style = AppTheme.typography.bodyMd,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    AppTheme {
        SettingsContent(
            viewState = ViewState(refreshInterval = RefreshIntervalUi.S5),
            onViewAction = {},
        )
    }
}
