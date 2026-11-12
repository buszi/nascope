package io.buszi.nascope.ui.common.util

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.common.theme.AppDimensions
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Grows the element to at least [AppDimensions.minTouchTarget] in both directions on
 * [WindowWidthClass.Compact] windows, where input is by finger. Denser sizes stay untouched on
 * Medium and Expanded windows, where input is by pointer.
 *
 * Apply it **before** any fixed `height(...)` so the minimum wins over the dense component height.
 */
@Composable
fun Modifier.minimumTouchTarget(): Modifier =
    if (AppTheme.windowWidthClass == WindowWidthClass.Compact) {
        sizeIn(minWidth = AppTheme.dimensions.minTouchTarget, minHeight = AppTheme.dimensions.minTouchTarget)
    } else {
        this
    }
