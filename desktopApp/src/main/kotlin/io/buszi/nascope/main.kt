package io.buszi.nascope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.buszi.nascope.di.initNascope
import java.awt.Taskbar
import javax.imageio.ImageIO

private const val IconResource = "/icon.png"

/** Set to `true` by the Gradle `run` task only (see `DesktopApplicationPlugin`); packaged apps are release builds. */
private const val DebugProperty = "nascope.debug"

/** Matches the app shell's bar color, so the strip behind the macOS title bar blends into the top bar. */
private val TitleBarStrip = Color(0xFF12141A)

/** Height of the macOS title bar the content extends under; keeps the traffic lights clear of the app's own bars. */
private val MacTitleBarHeight = 28.dp

private val isMac = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

fun main() {
    if (isMac) {
        // Must be set before AWT starts. Dark title bar controls for the dark UI.
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    }
    initNascope(isDebug = System.getProperty(DebugProperty) == "true")

    val iconImage = checkNotNull(object {}.javaClass.getResourceAsStream(IconResource)) { "Missing $IconResource" }
        .use(ImageIO::read)
    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
        // The dock icon when running unpackaged (./gradlew run); packaged builds use the bundle icon.
        runCatching { Taskbar.getTaskbar().iconImage = iconImage }
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "nascope",
            icon = BitmapPainter(iconImage.toComposeImageBitmap()),
        ) {
            if (isMac) {
                // Transparent title bar with the content drawn under it: no white bar, the traffic lights float.
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TitleBarStrip)
                    .padding(top = if (isMac) MacTitleBarHeight else 0.dp),
            ) {
                App()
            }
        }
    }
}
