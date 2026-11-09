package io.buszi.nascope

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.DashboardScreen

/**
 * Main application entry point composable.
 *
 * Applies the [AppTheme] and hosts the active screen scaffold.
 * In subsequent milestones, this will be wrapped by the navigation framework.
 */
@Composable
@Preview
fun App() {
    AppTheme {
        DashboardScreen()
    }
}
