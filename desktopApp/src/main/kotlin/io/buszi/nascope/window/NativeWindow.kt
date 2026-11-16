package io.buszi.nascope.window

import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener

/**
 * Runs [action] once this window has its native window: right away if it already has one, otherwise as soon as AWT
 * creates it, which is before the window is first shown. Styling applied then is in place on the first frame, with no
 * flash of the default look.
 */
internal fun Window.whenNativeWindowExists(action: () -> Unit) {
    if (isDisplayable) {
        action()
        return
    }
    addHierarchyListener(
        object : HierarchyListener {
            override fun hierarchyChanged(event: HierarchyEvent) {
                val displayabilityChanged = event.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
                if (displayabilityChanged && isDisplayable) {
                    removeHierarchyListener(this)
                    action()
                }
            }
        },
    )
}
