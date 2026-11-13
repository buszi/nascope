package io.buszi.nascope.ui.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * System insets (navigation bar, display cutout) that the workspace content must keep clear of.
 *
 * The app shell does not pad the workspace itself, so scrollable content (the dashboard bricks) can draw
 * behind a navigation bar docked at the side of a rotated phone and pad its own content by these insets.
 * Screens that do not scroll under the bars apply them with `windowInsetsPadding`.
 */
val LocalContentInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }

/**
 * System bars and display cutout, without the on-screen keyboard: what the app shell keeps its bars and the
 * workspace clear of. The keyboard is left out on purpose, so the bars stay where they are and the window keeps
 * its size; a screen with text fields makes room for the keyboard inside its own scrolling content
 * (`CenteredPanel`'s `imePadding` spacer).
 */
val WindowInsets.Companion.safeArea: WindowInsets
    @Composable get() = systemBars.union(displayCutout)
