package io.buszi.nascope.window

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference
import io.github.aakira.napier.Napier

/**
 * Linux: the window manager draws the title bar, so the window asks it for the dark variant of its theme through the
 * `_GTK_THEME_VARIANT` X11 property, as GTK apps do. GNOME (Mutter) honors it, also under Wayland, where the JVM's
 * windows run on XWayland; window managers that ignore it keep their light title bar.
 */
internal object LinuxWindowChrome : WindowChrome {

    override fun style(window: ComposeWindow) {
        window.whenNativeWindowExists {
            runCatching { setDarkThemeVariant(Native.getWindowID(window)) }
                .onFailure { Napier.w("Could not style the Linux title bar", it) }
        }
    }

    /**
     * [awtWindowId] is the window AWT draws into, a child of the top-level window the window manager decorates; the
     * property goes on the latter. Uses its own X connection; window ids are global to the X server.
     */
    private fun setDarkThemeVariant(awtWindowId: Long) {
        val x11 = X11.INSTANCE
        val display = checkNotNull(x11.XOpenDisplay(null)) { "No X display" }
        try {
            val topLevel = x11.clientTopLevel(display, X11.Window(awtWindowId))
            val value = DarkVariant.encodeToByteArray()
            val memory = Memory(value.size.toLong()).apply { write(0, value, 0, value.size) }
            x11.XChangeProperty(
                display,
                topLevel,
                x11.XInternAtom(display, "_GTK_THEME_VARIANT", false),
                x11.XInternAtom(display, "UTF8_STRING", false),
                8,
                X11.PropModeReplace,
                memory,
                value.size,
            )
            x11.XFlush(display)
        } finally {
            x11.XCloseDisplay(display)
        }
    }

    /**
     * The nearest ancestor (or [window] itself) that carries `WM_CLASS`, which AWT sets on its top-level windows.
     * Falls back to the last window below the root, which is the top-level as long as the window is not mapped yet.
     */
    private fun X11.clientTopLevel(display: X11.Display, window: X11.Window): X11.Window {
        var current = window
        repeat(MaxAncestorDepth) {
            if (hasProperty(display, current, X11.XA_WM_CLASS)) return current
            current = parentBelowRoot(display, current) ?: return current
        }
        return current
    }

    /** The parent of [window], or `null` when the parent is the root window (or the query fails). */
    private fun X11.parentBelowRoot(display: X11.Display, window: X11.Window): X11.Window? {
        val root = X11.WindowByReference()
        val parent = X11.WindowByReference()
        val children = PointerByReference()
        val childCount = IntByReference()
        if (XQueryTree(display, window, root, parent, children, childCount) == 0) return null
        children.value?.let(::XFree)
        val parentWindow = parent.value
        return parentWindow.takeUnless { it == null || it.toLong() == 0L || it.toLong() == root.value?.toLong() }
    }

    private fun X11.hasProperty(display: X11.Display, window: X11.Window, property: X11.Atom): Boolean {
        val actualType = X11.AtomByReference()
        val data = PointerByReference()
        val status = XGetWindowProperty(
            display,
            window,
            property,
            NativeLong(0),
            NativeLong(0),
            false,
            X11.Atom(X11.AnyPropertyType.toLong()),
            actualType,
            IntByReference(),
            NativeLongByReference(),
            NativeLongByReference(),
            data,
        )
        data.value?.let(::XFree)
        return status == 0 && actualType.value?.toLong()?.let { it != 0L } == true
    }

    private const val DarkVariant = "dark"

    /** AWT nests its windows two or three deep; the bound only guards against a malformed tree. */
    private const val MaxAncestorDepth = 8
}
