package io.buszi.nascope.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The app shell's top bar color; native title bars take it (or come as close as the OS allows) to blend in. */
internal val TitleBarColor = Color(0xFF12141A)

/**
 * The native window frame around the app, per operating system. The app is always dark, while every OS draws its
 * title bar in the light system look unless asked otherwise; each implementation asks in its own way.
 *
 * `main` picks one with [forCurrentOs], calls [beforeAwtStart] first thing and [style] once the window exists.
 */
internal interface WindowChrome {

    /** Space the content leaves free at the top, for a title bar that is drawn over the content. */
    val contentTopInset: Dp get() = 0.dp

    /** Runs before AWT starts, for settings AWT only reads at startup. */
    fun beforeAwtStart() = Unit

    /**
     * Styles [window]. Called while the window's content is first composed, which can be before the native window
     * exists: implementations that need it wait for it (see [whenNativeWindowExists]). Must never throw: a frame that
     * cannot be styled stays in the system look.
     */
    fun style(window: ComposeWindow)

    companion object {

        fun forCurrentOs(): WindowChrome {
            val osName = System.getProperty("os.name").orEmpty()
            return when {
                osName.startsWith("Mac", ignoreCase = true) -> MacWindowChrome
                osName.startsWith("Windows", ignoreCase = true) -> WindowsWindowChrome
                osName.startsWith("Linux", ignoreCase = true) -> LinuxWindowChrome
                else -> SystemWindowChrome
            }
        }
    }
}

/** Leaves the frame in the system look, for operating systems without a dedicated implementation. */
internal object SystemWindowChrome : WindowChrome {

    override fun style(window: ComposeWindow) = Unit
}
