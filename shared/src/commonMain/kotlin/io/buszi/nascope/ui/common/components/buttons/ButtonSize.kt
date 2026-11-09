package io.buszi.nascope.ui.common.components.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Height cadence variants for telemetry action buttons.
 *
 * - [Dense]: 32dp compact height ([AppTheme.dimensions.buttonHeightDense]), matching [AppTheme.dimensions.inputHeight]
 *   for inline data tables, filter toolbars, and card header actions.
 * - [Regular]: 36dp standard height ([AppTheme.dimensions.buttonHeightRegular]) for prominent forms, modal dialogs,
 *   and primary screen actions.
 */
enum class ButtonSize {
    Dense,
    Regular;

    val height: Dp
        @Composable
        get() = when (this) {
            Dense -> AppTheme.dimensions.buttonHeightDense
            Regular -> AppTheme.dimensions.buttonHeightRegular
        }
}
