package io.buszi.nascope.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalWindowWidthClass
import io.buszi.nascope.ui.common.util.toWindowWidthClass

/**
 * Setup flow shown while no server is configured. It is rendered outside the navigation scaffold, so
 * there is no rail, top bar or bottom bar, and keeps its own Navigation 3 back stack: system back
 * and Escape go from the connection form back to the welcome screen.
 */
@Composable
fun SetupFlow(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(SetupRouteSavedStateConfiguration, SetupRoute.Welcome)
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    // Escape only reaches this flow while it holds focus; take it back whenever nothing inside has it.
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(hasFocus, backStack.size) {
        if (!hasFocus) runCatching { focusRequester.requestFocus() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvasBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && backStack.size > 1) {
                    navigateBack()
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable(),
    ) {
        CompositionLocalProvider(LocalWindowWidthClass provides maxWidth.toWindowWidthClass()) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = navigateBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<SetupRoute.Welcome> {
                        WelcomeScreen(onConnectToServer = { backStack.add(SetupRoute.Connection) })
                    }
                    entry<SetupRoute.Connection> {
                        // Placeholder until the connection editor replaces it.
                        ConnectionPlaceholderScreen(onBack = navigateBack)
                    }
                },
            )
        }
    }
}
