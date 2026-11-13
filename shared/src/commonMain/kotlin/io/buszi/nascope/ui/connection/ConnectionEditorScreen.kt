package io.buszi.nascope.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_action_back
import io.buszi.nascope.resources.connection_action_saving
import io.buszi.nascope.resources.connection_action_submit
import io.buszi.nascope.resources.connection_action_testing
import io.buszi.nascope.resources.connection_alias_placeholder
import io.buszi.nascope.resources.connection_auth_toggle
import io.buszi.nascope.resources.connection_checks_title
import io.buszi.nascope.resources.connection_field_alias
import io.buszi.nascope.resources.connection_field_host
import io.buszi.nascope.resources.connection_field_password
import io.buszi.nascope.resources.connection_field_port
import io.buszi.nascope.resources.connection_field_protocol
import io.buszi.nascope.resources.connection_field_username
import io.buszi.nascope.resources.connection_host_helper
import io.buszi.nascope.resources.connection_host_placeholder
import io.buszi.nascope.resources.connection_password_hide
import io.buszi.nascope.resources.connection_password_keep_helper
import io.buszi.nascope.resources.connection_password_missing
import io.buszi.nascope.resources.connection_password_show
import io.buszi.nascope.resources.connection_port_helper
import io.buszi.nascope.resources.connection_secret_not_persisted
import io.buszi.nascope.resources.connection_title_edit
import io.buszi.nascope.resources.connection_title_new
import io.buszi.nascope.resources.connection_trusted_certificate
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.CenteredPanel
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.components.inputs.FormField
import io.buszi.nascope.ui.common.components.inputs.PasswordTextField
import io.buszi.nascope.ui.common.components.inputs.SegmentedSelector
import io.buszi.nascope.ui.common.components.inputs.TelemetrySwitch
import io.buszi.nascope.ui.common.components.inputs.TelemetryTextField
import io.buszi.nascope.ui.common.model.CertificatePromptUi
import io.buszi.nascope.ui.common.model.ConnectionCheckStatusUi
import io.buszi.nascope.ui.common.model.ConnectionCheckStepUi
import io.buszi.nascope.ui.common.model.ConnectionCheckUi
import io.buszi.nascope.ui.common.model.ConnectionFailureUi
import io.buszi.nascope.ui.common.model.FormErrorUi
import io.buszi.nascope.ui.common.model.ServerCertificateUi
import io.buszi.nascope.ui.common.model.ServerProtocolUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.Phase
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConnectionEditorScreen(
    profileId: String? = null,
    onBack: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val viewModel = koinViewModel<ConnectionEditorViewModel>(parameters = { parametersOf(profileId) })
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(viewState.phase) {
        if (viewState.phase == Phase.Saved) onSaved()
    }

    ConnectionEditorContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
        onBack = onBack,
    )
}

@Composable
fun ConnectionEditorContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val usernameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(viewState.usernameFocusRequest) {
        if (viewState.usernameFocusRequest > 0) runCatching { usernameFocusRequester.requestFocus() }
    }
    val passwordFocusRequester = remember { FocusRequester() }
    LaunchedEffect(viewState.passwordFocusRequest) {
        if (viewState.passwordFocusRequest > 0) runCatching { passwordFocusRequester.requestFocus() }
    }

    CenteredPanel(
        modifier = Modifier
            .background(AppTheme.colors.canvasBackground)
            .testTag(TestTags.ConnectionScreen),
    ) {
        Text(
            text = stringResource(if (viewState.isEditMode) Res.string.connection_title_edit else Res.string.connection_title_new),
            style = AppTheme.typography.headlineLg,
            color = AppTheme.colors.textPrimary,
        )

        FormField(
            label = stringResource(Res.string.connection_field_alias),
            errorText = viewState.aliasError?.message(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TelemetryTextField(
                value = viewState.alias,
                onValueChange = { onViewAction(ViewAction.OnAliasChange(it)) },
                enabled = viewState.isEditable,
                isError = viewState.aliasError != null,
                placeholder = stringResource(Res.string.connection_alias_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.ConnectionAlias),
            )
        }

        FormField(
            label = stringResource(Res.string.connection_field_protocol),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SegmentedSelector(
                options = ServerProtocolUi.entries,
                selected = viewState.protocol,
                onSelect = { onViewAction(ViewAction.OnProtocolSelect(it)) },
                optionLabel = ServerProtocolUi::label,
                enabled = viewState.isEditable,
                optionTestTag = { TestTags.connectionProtocol(it.name) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FormField(
            label = stringResource(Res.string.connection_field_host),
            helperText = stringResource(Res.string.connection_host_helper),
            errorText = viewState.hostError?.message(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TelemetryTextField(
                value = viewState.host,
                onValueChange = { onViewAction(ViewAction.OnHostChange(it)) },
                enabled = viewState.isEditable,
                isError = viewState.hostError != null,
                placeholder = stringResource(Res.string.connection_host_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.ConnectionHost),
            )
        }

        FormField(
            label = stringResource(Res.string.connection_field_port),
            helperText = stringResource(Res.string.connection_port_helper),
            errorText = viewState.portError?.message(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TelemetryTextField(
                value = viewState.port,
                onValueChange = { onViewAction(ViewAction.OnPortChange(it)) },
                enabled = viewState.isEditable,
                isError = viewState.portError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.ConnectionPort),
            )
        }

        viewState.trustedCertificate?.let { fingerprint ->
            Text(
                text = stringResource(
                    Res.string.connection_trusted_certificate,
                    ServerCertificateUi.wrappableFingerprint(fingerprint),
                ),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.testTag(TestTags.ConnectionTrustedCertificate),
            )
        }

        TelemetrySwitch(
            checked = viewState.authEnabled,
            onCheckedChange = { onViewAction(ViewAction.OnAuthToggle(it)) },
            enabled = viewState.isEditable,
            label = stringResource(Res.string.connection_auth_toggle),
            modifier = Modifier.testTag(TestTags.ConnectionAuthToggle),
        )

        if (viewState.authEnabled) {
            FormField(
                label = stringResource(Res.string.connection_field_username),
                errorText = viewState.usernameError?.message(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TelemetryTextField(
                    value = viewState.username,
                    onValueChange = { onViewAction(ViewAction.OnUsernameChange(it)) },
                    enabled = viewState.isEditable,
                    isError = viewState.usernameError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .focusRequester(usernameFocusRequester)
                        .testTag(TestTags.ConnectionUsername),
                )
            }

            FormField(
                label = stringResource(Res.string.connection_field_password),
                helperText = if (viewState.isEditMode && !viewState.showPasswordMissing) {
                    stringResource(Res.string.connection_password_keep_helper)
                } else {
                    null
                },
                errorText = if (viewState.showPasswordMissing) stringResource(Res.string.connection_password_missing) else null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PasswordTextField(
                    value = viewState.password,
                    onValueChange = { onViewAction(ViewAction.OnPasswordChange(it)) },
                    showLabel = stringResource(Res.string.connection_password_show),
                    hideLabel = stringResource(Res.string.connection_password_hide),
                    enabled = viewState.isEditable,
                    isError = viewState.showPasswordMissing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onViewAction(ViewAction.OnSubmit) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .testTag(TestTags.ConnectionPassword),
                )
            }
        }

        TelemetryCard(
            title = stringResource(Res.string.connection_checks_title),
            modifier = Modifier.fillMaxWidth(),
        ) {
            viewState.checks.forEach { ConnectionCheckRow(it) }
        }

        viewState.certificatePrompt?.let { prompt ->
            CertificatePromptCard(
                prompt = prompt,
                enabled = viewState.canAnswerCertificatePrompt,
                onTrust = { onViewAction(ViewAction.OnTrustCertificate) },
                onDismiss = { onViewAction(ViewAction.OnDismissCertificate) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (viewState.showSecretNotPersistedNotice) {
            Text(
                text = stringResource(Res.string.connection_secret_not_persisted),
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.degraded,
            )
        }

        PrimaryButton(
            text = stringResource(
                when (viewState.phase) {
                    Phase.Idle, Phase.Saved -> Res.string.connection_action_submit
                    Phase.Testing -> Res.string.connection_action_testing
                    Phase.Saving -> Res.string.connection_action_saving
                },
            ),
            onClick = { onViewAction(ViewAction.OnSubmit) },
            size = ButtonSize.Regular,
            enabled = viewState.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .testTag(TestTags.ConnectionSubmit),
        )

        if (onBack != null) {
            SecondaryButton(
                text = stringResource(Res.string.connection_action_back),
                onClick = onBack,
                size = ButtonSize.Regular,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.ConnectionBack),
            )
        }
    }
}

@Composable
private fun FormErrorUi.message(): String = stringResource(messageRes)

@Preview
@Composable
private fun ConnectionEditorContentNewPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(),
            onViewAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ConnectionEditorContentErrorsPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(
                alias = "",
                host = "http://alpha-node-01.local/",
                port = "70000",
                hostError = FormErrorUi.HostContainsScheme,
                aliasError = FormErrorUi.AliasBlank,
                portError = FormErrorUi.PortOutOfRange,
            ),
            onViewAction = {},
        )
    }
}

@Preview
@Composable
private fun ConnectionEditorContentPasswordMissingPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(
                alias = "NAS rack 2",
                host = "alpha-node-01.local",
                authEnabled = true,
                isEditMode = true,
                passwordMissing = true,
            ),
            onViewAction = {},
        )
    }
}

@Preview
@Composable
private fun ConnectionEditorContentTestingPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(
                alias = "NAS rack 2",
                host = "alpha-node-01.local",
                authEnabled = true,
                password = "secret",
                phase = Phase.Testing,
                checks = listOf(
                    ConnectionCheckUi(ConnectionCheckStepUi.ReachHost, ConnectionCheckStatusUi.Passed),
                    ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, ConnectionCheckStatusUi.Passed),
                    ConnectionCheckUi(ConnectionCheckStepUi.Authenticate, ConnectionCheckStatusUi.Running),
                ),
            ),
            onViewAction = {},
        )
    }
}

@Preview
@Composable
private fun ConnectionEditorContentFailedPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(
                alias = "NAS rack 2",
                host = "alpha-node-01.local",
                authEnabled = true,
                isEditMode = true,
                checks = listOf(
                    ConnectionCheckUi(ConnectionCheckStepUi.ReachHost, ConnectionCheckStatusUi.Passed),
                    ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, ConnectionCheckStatusUi.Passed),
                    ConnectionCheckUi(
                        step = ConnectionCheckStepUi.Authenticate,
                        status = ConnectionCheckStatusUi.Failed,
                        failure = ConnectionFailureUi.Unauthorized,
                    ),
                ),
                showSecretNotPersistedNotice = true,
            ),
            onViewAction = {},
        )
    }
}

@Preview
@Composable
private fun ConnectionEditorContentCertificatePromptPreview() {
    AppTheme {
        ConnectionEditorContent(
            viewState = ViewState(
                alias = "NAS rack 2",
                protocol = ServerProtocolUi.Https,
                host = "192.168.1.20",
                port = "8443",
                checks = listOf(
                    ConnectionCheckUi(
                        step = ConnectionCheckStepUi.ReachHost,
                        status = ConnectionCheckStatusUi.Failed,
                        failure = ConnectionFailureUi.UntrustedCertificate,
                    ),
                    ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, ConnectionCheckStatusUi.Pending),
                    ConnectionCheckUi(ConnectionCheckStepUi.Authenticate, ConnectionCheckStatusUi.Pending),
                ),
                certificatePrompt = CertificatePromptUi(
                    certificate = ServerCertificateUi(
                        fingerprint = "37:B1:BD:A5:AD:60:02:0C:B0:8F:05:67:65:F4:02:F0:AC:0C:90:34:C8:AA:20:42:50:22:79:E3:76:00:CA:E2",
                        subject = "CN=nas.local",
                        issuer = "CN=nas.local",
                        validUntil = "2027-03-01",
                        isSelfSigned = true,
                    ),
                    isChange = false,
                ),
            ),
            onViewAction = {},
        )
    }
}
