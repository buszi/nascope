package io.buszi.nascope.ui.setup

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.setup_action_back
import io.buszi.nascope.resources.setup_connection_placeholder
import io.buszi.nascope.resources.setup_connection_title
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.CenteredPanel
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import org.jetbrains.compose.resources.stringResource

/**
 * Stand-in for `SetupRoute.Connection` until the connection editor exists.
 */
@Composable
fun ConnectionPlaceholderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredPanel(modifier = modifier.background(AppTheme.colors.canvasBackground)) {
        Text(
            text = stringResource(Res.string.setup_connection_title),
            style = AppTheme.typography.headlineLg,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(Res.string.setup_connection_placeholder),
            style = AppTheme.typography.bodyMd,
            color = AppTheme.colors.textSecondary,
        )
        SecondaryButton(
            text = stringResource(Res.string.setup_action_back),
            onClick = onBack,
            size = ButtonSize.Regular,
            modifier = Modifier
                .minimumTouchTarget()
                .testTag(TestTags.SetupBack),
        )
    }
}

@Preview
@Composable
private fun ConnectionPlaceholderScreenPreview() {
    AppTheme {
        ConnectionPlaceholderScreen(onBack = {})
    }
}
