package io.buszi.nascope

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.di.NascopeApp
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.NascopeAppScaffold
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * Main application entry point composable.
 *
 * Applies Koin dependency injection, [AppTheme], and hosts the adaptive Navigation 3 application scaffold.
 */
@Composable
@Preview
fun App() {
    KoinApplication(
        configuration = koinConfiguration<NascopeApp>(),
    ) {
        AppTheme {
            NascopeAppScaffold()
        }
    }
}
