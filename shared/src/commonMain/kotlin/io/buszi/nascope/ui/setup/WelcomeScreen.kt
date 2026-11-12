package io.buszi.nascope.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.welcome_action_connect
import io.buszi.nascope.resources.welcome_action_demo
import io.buszi.nascope.resources.welcome_description
import io.buszi.nascope.resources.welcome_title
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.CenteredPanel
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalWindowWidthClass
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.setup.WelcomeViewModel.ViewAction
import io.buszi.nascope.ui.setup.WelcomeViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeScreen(
    onConnectToServer: () -> Unit,
) {
    val viewModel = koinViewModel<WelcomeViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    WelcomeContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
        onConnectToServer = onConnectToServer,
    )
}

@Composable
fun WelcomeContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    onConnectToServer: () -> Unit,
) {
    CenteredPanel(
        modifier = Modifier
            .background(AppTheme.colors.canvasBackground)
            .testTag(TestTags.WelcomeScreen),
    ) {
        Text(
            text = stringResource(Res.string.welcome_title),
            style = if (LocalWindowWidthClass.current == WindowWidthClass.Compact) {
                AppTheme.typography.headlineXlMobile
            } else {
                AppTheme.typography.headlineXl
            },
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(Res.string.welcome_description),
            style = AppTheme.typography.bodyLg,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
        PrimaryButton(
            text = stringResource(Res.string.welcome_action_connect),
            onClick = onConnectToServer,
            size = ButtonSize.Regular,
            enabled = !viewState.isStartingDemo,
            modifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .testTag(TestTags.WelcomeConnect),
        )
        SecondaryButton(
            text = stringResource(Res.string.welcome_action_demo),
            onClick = { onViewAction(ViewAction.OnExploreDemo) },
            size = ButtonSize.Regular,
            enabled = !viewState.isStartingDemo,
            modifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .testTag(TestTags.WelcomeDemo),
        )
    }
}

@Preview
@Composable
private fun WelcomeContentPreview() {
    AppTheme {
        WelcomeContent(
            viewState = ViewState(),
            onViewAction = {},
            onConnectToServer = {},
        )
    }
}

@Preview
@Composable
private fun WelcomeContentStartingDemoPreview() {
    AppTheme {
        WelcomeContent(
            viewState = ViewState(isStartingDemo = true),
            onViewAction = {},
            onConnectToServer = {},
        )
    }
}
