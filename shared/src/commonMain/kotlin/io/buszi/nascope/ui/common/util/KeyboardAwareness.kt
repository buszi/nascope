 package io.buszi.nascope.ui.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Keeps a focused text field (or a container holding one, such as a form field with its hint) above the
 * on-screen keyboard.
 *
 * The window keeps its size when the keyboard opens (the shell leaves the keyboard out of its insets), so the
 * scroll container's viewport still reaches under the keyboard and a plain "bring into view" sees nothing to
 * do. While the field is focused, this asks for its own area plus the part of the keyboard over the content
 * (the keyboard inset minus what the ancestors already consumed: the same height `imePadding` gives the
 * screen's spacer) plus a small gap, on every change of the keyboard's height. A field covered by the keyboard
 * scrolls to just above it, following the keyboard as it animates in; a field already above it stays put.
 * Without an on-screen keyboard (desktop, hardware keyboard) the inset is 0 and nothing happens.
 */
@Composable
fun Modifier.keepInViewAboveKeyboard(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val gap = with(density) { KeyboardGap.toPx() }
    var focused by remember { mutableStateOf(false) }
    var consumed by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        snapshotFlow { (ime.getBottom(density) - consumed.getBottom(density)).coerceAtLeast(0) }
            .distinctUntilChanged()
            .filter { it > 0 }
            .collect { overlap ->
                requester.bringIntoView(Rect(0f, 0f, size.width.toFloat(), size.height + overlap + gap))
            }
    }

    return this
        .onConsumedWindowInsetsChanged { consumed = it }
        .onSizeChanged { size = it }
        .bringIntoViewRequester(requester)
        .onFocusChanged { focused = it.hasFocus }
}

/** Space kept between a focused field and the top of the keyboard. */
private val KeyboardGap = 16.dp
