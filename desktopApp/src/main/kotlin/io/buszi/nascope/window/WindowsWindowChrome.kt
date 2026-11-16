package io.buszi.nascope.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import io.github.aakira.napier.Napier

/**
 * Windows: the native title bar stays (snap layouts, resizing and the caption buttons keep working) and the Desktop
 * Window Manager draws it dark. Windows 11 also takes the exact [TitleBarColor]; Windows 10 (20H1 and later) draws
 * its own dark gray, older versions stay light.
 */
internal object WindowsWindowChrome : WindowChrome {

    // DWMWINDOWATTRIBUTE values from dwmapi.h.
    private const val UseImmersiveDarkMode = 20
    private const val CaptionColor = 35

    override fun style(window: ComposeWindow) {
        window.whenNativeWindowExists {
            runCatching {
                val hwnd = HWND(Native.getWindowPointer(window))
                // The results are not checked: a Windows version without an attribute rejects it and keeps its default.
                Dwmapi.setAttribute(hwnd, UseImmersiveDarkMode, 1)
                Dwmapi.setAttribute(hwnd, CaptionColor, TitleBarColor.toColorRef())
            }.onFailure { Napier.w("Could not style the Windows title bar", it) }
        }
    }

    /** A Win32 COLORREF, `0x00BBGGRR`. */
    private fun Color.toColorRef(): Int {
        val argb = toArgb()
        val red = argb shr 16 and 0xFF
        val green = argb shr 8 and 0xFF
        val blue = argb and 0xFF
        return blue shl 16 or (green shl 8) or red
    }
}

/** The one Desktop Window Manager call the title bar needs. Kept from shrinking by the JNA rules in proguard-rules.pro. */
@Suppress("FunctionName")
private interface DwmapiLibrary : Library {

    fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: IntByReference, size: Int): Int
}

private object Dwmapi {

    private val library: DwmapiLibrary by lazy { Native.load("dwmapi", DwmapiLibrary::class.java) }

    /** Sets a DWORD-sized window attribute; returns the HRESULT. */
    fun setAttribute(hwnd: HWND, attribute: Int, value: Int): Int =
        library.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), Int.SIZE_BYTES)
}
