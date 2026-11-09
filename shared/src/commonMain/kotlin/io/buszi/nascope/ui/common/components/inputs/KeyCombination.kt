package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.runtime.Immutable

/**
 * Ordered sequence of [ShortcutKey]s evaluated together as a keyboard shortcut.
 *
 * Provides fluent factories for common telemetry shortcuts:
 * ```kotlin
 * val searchShortcut = KeyCombination.commandOrControl("K")
 * val pauseShortcut = KeyCombination.single(ShortcutKey.Space)
 * ```
 */
@Immutable
data class KeyCombination(
    val keys: List<ShortcutKey>,
) {
    constructor(vararg keys: ShortcutKey) : this(keys.toList())

    companion object {

        /**
         * Shortcut with primary modifier ([ShortcutKey.CommandOrControl]) plus [additionalKeys].
         */
        fun commandOrControl(vararg additionalKeys: String): KeyCombination =
            KeyCombination(listOf(ShortcutKey.CommandOrControl) + additionalKeys.map { ShortcutKey.Custom(it) })

        /**
         * Shortcut with primary modifier ([ShortcutKey.CommandOrControl]) plus [additionalKeys].
         */
        fun commandOrControl(vararg additionalKeys: ShortcutKey): KeyCombination =
            KeyCombination(listOf(ShortcutKey.CommandOrControl) + additionalKeys)

        /**
         * Constructs a combination from arbitrary [ShortcutKey]s.
         */
        fun of(vararg keys: ShortcutKey): KeyCombination =
            KeyCombination(keys.toList())

        /**
         * Constructs a combination from literal key names.
         */
        fun of(vararg keys: String): KeyCombination =
            KeyCombination(keys.map { ShortcutKey.Custom(it) })

        /**
         * Single-key shortcut combination.
         */
        fun single(key: String): KeyCombination =
            KeyCombination(listOf(ShortcutKey.Custom(key)))

        /**
         * Single-key shortcut combination.
         */
        fun single(key: ShortcutKey): KeyCombination =
            KeyCombination(listOf(key))
    }
}
