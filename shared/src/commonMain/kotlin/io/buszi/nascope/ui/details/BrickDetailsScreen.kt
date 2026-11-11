package io.buszi.nascope.ui.details

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
import io.buszi.nascope.resources.screen_details_close
import io.buszi.nascope.resources.screen_details_description
import io.buszi.nascope.resources.screen_details_header
import io.buszi.nascope.resources.screen_details_route_tag
import io.buszi.nascope.resources.screen_details_title
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import org.jetbrains.compose.resources.stringResource

/**
 * Mockup detail screen / inspector pane for [io.buszi.nascope.ui.navigation.NavRoute.Details].
 *
 * Displays an identification readout indicating which [brickId] detail view was opened,
 * along with an action to close/pop the detail route.
 *
 * @param brickId Identifier of the telemetry brick being inspected.
 * @param onClose Callback invoked when the user closes or pops the detail view.
 * @param modifier Root layout modifier applied to the detail container.
 */
@Composable
fun BrickDetailsScreen(
    brickId: DashboardBrickId,
    onClose: () -> Unit,
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
            title = stringResource(Res.string.screen_details_header),
            headerAction = {
                TagChip(text = stringResource(Res.string.screen_details_route_tag, brickId.name))
            },
        ) {
            Text(
                text = stringResource(Res.string.screen_details_title, brickId.name),
                style = AppTheme.typography.headlineLg,
                color = AppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
            Text(
                text = stringResource(Res.string.screen_details_description, brickId.name),
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceLg))
            SecondaryButton(
                text = stringResource(Res.string.screen_details_close),
                onClick = onClose,
            )
        }
    }
}

@Preview
@Composable
private fun BrickDetailsScreenPreview() {
    AppTheme {
        BrickDetailsScreen(
            brickId = DashboardBrickId.CpuMatrix,
            onClose = {},
        )
    }
}
