package io.buszi.nascope.ui.common.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.util.keepInViewAboveKeyboard
import io.buszi.nascope.ui.common.theme.AppTheme

/**
 * Labelled form row: a caption above the input [content] and an optional supporting line below it.
 *
 * The supporting line shows [errorText] when present, otherwise [helperText]; an error replaces the
 * helper instead of stacking with it, so the row keeps a stable height. Errors are announced to
 * screen readers as they appear.
 *
 * @param label Caption rendered above the input in [AppTheme.typography.labelUi].
 * @param modifier Root layout modifier.
 * @param helperText Optional hint shown below the input while there is no error.
 * @param errorText Optional validation message; replaces [helperText] and uses [errorColor].
 * @param labelColor Caption color, defaults to [AppTheme.colors.textSecondary].
 * @param helperColor Hint color, defaults to [AppTheme.colors.textMuted].
 * @param errorColor Error message color, defaults to [AppTheme.colors.critical].
 * @param content The input itself (e.g. [TelemetryTextField], [PasswordTextField], [SegmentedSelector]).
 */
@Composable
fun FormField(
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    labelColor: Color = AppTheme.colors.textSecondary,
    helperColor: Color = AppTheme.colors.textMuted,
    errorColor: Color = AppTheme.colors.critical,
    content: @Composable () -> Unit,
) {
    Column(
        // The whole field, hint or error included, stays above the keyboard while its input is focused.
        modifier = modifier.keepInViewAboveKeyboard(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelUi,
            color = labelColor,
            maxLines = 1,
        )
        content()
        when {
            errorText != null -> Text(
                text = errorText,
                style = AppTheme.typography.bodySm,
                color = errorColor,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            helperText != null -> Text(
                text = helperText,
                style = AppTheme.typography.bodySm,
                color = helperColor,
            )
        }
    }
}

@Preview
@Composable
private fun FormFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            FormField(
                label = "HOST",
                helperText = "Hostname or IP address, without http://",
                modifier = Modifier.fillMaxWidth(),
            ) {
                TelemetryTextField(value = "alpha-node-01.local", onValueChange = {}, modifier = Modifier.fillMaxWidth())
            }

            FormField(
                label = "HOST",
                helperText = "Hostname or IP address, without http://",
                modifier = Modifier.fillMaxWidth(),
            ) {
                TelemetryTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "192.168.1.120",
                    interactionSource = rememberFocusedInteractionSource(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormField(
                label = "PORT",
                helperText = "Glances default is 61208",
                errorText = "Port must be between 1 and 65535",
                modifier = Modifier.fillMaxWidth(),
            ) {
                TelemetryTextField(value = "70000", onValueChange = {}, isError = true, modifier = Modifier.fillMaxWidth())
            }

            FormField(label = "ALIAS", modifier = Modifier.fillMaxWidth()) {
                TelemetryTextField(value = "NAS rack 2", onValueChange = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
