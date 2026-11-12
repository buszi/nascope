package io.buszi.nascope

import androidx.compose.runtime.Composable
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.root.AppRoot

/**
 * Main application entry point composable.
 *
 * Applies [AppTheme] and hosts the root gate, which shows the setup flow or the adaptive navigation scaffold.
 * Koin must already be running: platform entry points call [io.buszi.nascope.di.initNascope] first.
 */
@Composable
fun App() {
    AppTheme {
        AppRoot()
    }
}
