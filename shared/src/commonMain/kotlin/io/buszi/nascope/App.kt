package io.buszi.nascope

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.di.NascopeApp
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.DashboardScreen
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * Main application entry point composable.
 *
 * Applies Koin dependency injection, [AppTheme], and hosts the active screen scaffold.
 * In subsequent milestones, this will be wrapped by the navigation framework.
 */
@Composable
@Preview
fun App() {
    KoinApplication(
        configuration = koinConfiguration<NascopeApp>(),
    ) {
        AppTheme {
            DashboardScreen()
        }
    }
}
