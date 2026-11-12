package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/** An [MutableInteractionSource] that reports keyboard focus, so previews can show the focused state. */
@Composable
internal fun rememberFocusedInteractionSource(): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.emit(FocusInteraction.Focus())
    }
    return interactionSource
}
