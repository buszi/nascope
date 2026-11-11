package io.buszi.nascope

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.buszi.nascope.di.initNascope

fun main() {
    initNascope()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "nascope",
        ) {
            App()
        }
    }
}
