package io.buszi.nascope.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.ui.common.testing.testTagsAsResourceId
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.NascopeAppScaffold
import io.buszi.nascope.ui.root.AppRootViewModel.Destination
import io.buszi.nascope.ui.root.AppRootViewModel.ViewState
import io.buszi.nascope.ui.setup.SetupFlow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppRoot() {
    val viewModel = koinViewModel<AppRootViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    AppRootContent(viewState = viewState)
}

@Composable
fun AppRootContent(viewState: ViewState) {
    Box(
        modifier = Modifier
            .testTagsAsResourceId()
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground),
    ) {
        when (viewState.destination) {
            Destination.Loading -> Unit
            Destination.Setup -> SetupFlow()
            Destination.Main -> NascopeAppScaffold()
        }
    }
}
