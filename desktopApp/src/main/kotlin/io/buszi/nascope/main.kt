package io.buszi.nascope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.buszi.nascope.di.initNascope
import io.buszi.nascope.window.TitleBarColor
import io.buszi.nascope.window.WindowChrome
import io.buszi.nascope.window.useCanvasBackground
import java.awt.Dimension
import java.awt.Taskbar
import javax.imageio.ImageIO

private const val IconResource = "/icon.png"

/** Set to `true` by the Gradle `run` task only (see `DesktopApplicationPlugin`); packaged apps are release builds. */
private const val DebugProperty = "nascope.debug"

/**
 * The smallest window the user can resize to, in AWT units (= dp at any display scale). Narrower than the phone
 * breakpoint on purpose, so the window can still be docked beside other windows in the phone layout; any smaller and
 * the bottom navigation and the bricks get cramped.
 */
private val MinWindowSize = Dimension(400, 600)

fun main() {
    val windowChrome = WindowChrome.forCurrentOs()
    windowChrome.beforeAwtStart()
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
            DisposableEffect(window) {
                window.minimumSize = MinWindowSize
                window.useCanvasBackground()
                windowChrome.style(window)
                onDispose { }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Shows where the content leaves room for a title bar drawn over it (macOS).
                    .background(TitleBarColor)
                    .padding(top = windowChrome.contentTopInset),
            ) {
                App()
            }
        }
    }
}
