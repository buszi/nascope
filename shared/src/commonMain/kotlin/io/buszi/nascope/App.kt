package io.buszi.nascope

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.common.testing.testTagsAsResourceId
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.NascopeAppScaffold

/**
 * Main application entry point composable.
 *
 * Applies [AppTheme] and hosts the adaptive Navigation 3 application scaffold.
 * Koin must already be running: platform entry points call [io.buszi.nascope.di.initNascope] first.
 */
@Composable
fun App() {
    AppTheme {
        NascopeAppScaffold(modifier = Modifier.testTagsAsResourceId())
    }
}
