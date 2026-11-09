package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.runtime.Immutable
import io.buszi.nascope.Platform

/**
 * Representation of a keyboard shortcut key or platform-adaptive modifier.
 *
 * Automatically resolves to the appropriate visual label or symbol according to the host platform:
 * - [CommandOrControl]: `"⌘"` on macOS, `"Ctrl"` on Windows/Linux.
 * - [OptionOrAlt]: `"⌥"` on macOS, `"Alt"` on Windows/Linux.
 * - [Shift]: `"⇧"` on macOS, `"Shift"` on Windows/Linux.
 * - [Control]: `"⌃"` on macOS, `"Ctrl"` on Windows/Linux.
 * - [Custom]: Literal static text (e.g. `"K"`, `"Space"`, `"Esc"`).
 */
@Immutable
sealed interface ShortcutKey {

    /**
     * Resolves the visual symbol or label for this key on the target [platform].
     */
    fun resolve(platform: Platform): String

    /**
     * Primary modifier key:
     * - macOS: Command glyph (`"⌘"`)
     * - Windows / Linux / others: `"Ctrl"`
     */
    data object CommandOrControl : ShortcutKey {
        override fun resolve(platform: Platform): String =
            if (platform.isMac) "⌘" else "Ctrl"
    }

    /**
     * Secondary modifier key:
     * - macOS: Option glyph (`"⌥"`)
     * - Windows / Linux / others: `"Alt"`
     */
    data object OptionOrAlt : ShortcutKey {
        override fun resolve(platform: Platform): String =
            if (platform.isMac) "⌥" else "Alt"
    }

    /**
     * Shift modifier key:
     * - macOS: Shift glyph (`"⇧"`)
     * - Windows / Linux / others: `"Shift"`
     */
    data object Shift : ShortcutKey {
        override fun resolve(platform: Platform): String =
            if (platform.isMac) "⇧" else "Shift"
    }

    /**
     * Control modifier key:
     * - macOS: Control glyph (`"⌃"`)
     * - Windows / Linux / others: `"Ctrl"`
     */
    data object Control : ShortcutKey {
        override fun resolve(platform: Platform): String =
            if (platform.isMac) "⌃" else "Ctrl"
    }

    /**
     * Literal key representation with a static [label] (e.g. `"K"`, `"Space"`, `"Esc"`).
     */
    data class Custom(val label: String) : ShortcutKey {
        override fun resolve(platform: Platform): String = label
    }

    companion object {
        fun key(label: String): ShortcutKey = Custom(label)
        val Space: ShortcutKey = Custom("Space")
        val Escape: ShortcutKey = Custom("Esc")
        val Enter: ShortcutKey = Custom("Enter")
        val Tab: ShortcutKey = Custom("Tab")
    }
}
