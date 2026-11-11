package io.buszi.nascope.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.screen_settings_description
import io.buszi.nascope.resources.screen_settings_header
import io.buszi.nascope.resources.screen_settings_route_tag
import io.buszi.nascope.resources.screen_settings_title
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Mockup screen for the [io.buszi.nascope.ui.navigation.NavRoute.Settings] destination.
 *
 * Displays an identification readout indicating that the Settings screen is open.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground)
            .padding(AppTheme.spacing.margin),
        contentAlignment = Alignment.TopStart,
    ) {
        TelemetryCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.screen_settings_header),
            headerAction = {
                TagChip(text = stringResource(Res.string.screen_settings_route_tag))
            },
        ) {
            Text(
                text = stringResource(Res.string.screen_settings_title),
                style = AppTheme.typography.headlineLg,
                color = AppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
            Text(
                text = stringResource(Res.string.screen_settings_description),
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen()
    }
}
