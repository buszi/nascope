package io.buszi.nascope.ui.common.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * - [sm]: 2dp (0.125rem)
 * - [base]: 4dp (0.25rem - default) - tags, inputs, buttons, menus
 * - [md]: 6dp (0.375rem)
 * - [lg]: 8dp (0.5rem) - telemetry panels, cards, terminals, dialogs
 * - [xl]: 12dp (0.75rem)
 * - [full]: 9999px - status dots (6px x 6px), counter badges
 */
@Immutable
data class AppShapes(
    val sm: CornerBasedShape = RoundedCornerShape(2.dp),
    val base: CornerBasedShape = RoundedCornerShape(4.dp),
    val md: CornerBasedShape = RoundedCornerShape(6.dp),
    val lg: CornerBasedShape = RoundedCornerShape(8.dp),
    val xl: CornerBasedShape = RoundedCornerShape(12.dp),
    val full: CornerBasedShape = CircleShape,
    val materialShapes: Shapes = Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp),
    ),
)

val DefaultAppShapes = AppShapes()

val LocalAppShapes = staticCompositionLocalOf { DefaultAppShapes }
