package io.buszi.nascope.ui.common.modifier

import androidx.compose.ui.Modifier

inline fun Modifier.optionalModifier(
    enabled: Boolean,
    modifier: Modifier.() -> Modifier,
): Modifier = if (enabled) { modifier() } else { this }
