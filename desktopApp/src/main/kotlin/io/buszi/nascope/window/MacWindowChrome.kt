package io.buszi.nascope.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * macOS: a transparent title bar with the content drawn under it, so there is no title bar strip at all and the traffic
 * lights float over the app's own top bar.
 */
internal object MacWindowChrome : WindowChrome {

    /** Height of the title bar the content extends under; keeps the traffic lights clear of the app's own bars. */
    override val contentTopInset: Dp = 28.dp

    override fun beforeAwtStart() {
        // Dark title bar controls for the dark UI; AWT reads it only at startup.
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    }

    override fun style(window: ComposeWindow) {
        with(window.rootPane) {
            putClientProperty("apple.awt.fullWindowContent", true)
            putClientProperty("apple.awt.transparentTitleBar", true)
            putClientProperty("apple.awt.windowTitleVisible", false)
        }
    }
}
