package io.buszi.nascope.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_certificate_body_changed
import io.buszi.nascope.resources.connection_certificate_body_untrusted
import io.buszi.nascope.resources.connection_certificate_dismiss
import io.buszi.nascope.resources.connection_certificate_fingerprint
import io.buszi.nascope.resources.connection_certificate_issuer
import io.buszi.nascope.resources.connection_certificate_self_signed
import io.buszi.nascope.resources.connection_certificate_subject
import io.buszi.nascope.resources.connection_certificate_title_changed
import io.buszi.nascope.resources.connection_certificate_title_untrusted
import io.buszi.nascope.resources.connection_certificate_trust
import io.buszi.nascope.resources.connection_certificate_trust_new
import io.buszi.nascope.resources.connection_certificate_valid_until
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.model.CertificatePromptUi
import io.buszi.nascope.ui.common.model.ServerCertificateUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import org.jetbrains.compose.resources.stringResource

/**
 * The certificate a connection test was refused with, and the choice to trust it. A changed certificate is
 * framed as a warning (critical border and colors), a first one as a question.
 */
@Composable
fun CertificatePromptCard(
    prompt: CertificatePromptUi,
    enabled: Boolean,
    onTrust: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (prompt.isChange) AppTheme.colors.critical else AppTheme.colors.degraded
    TelemetryCard(
        title = stringResource(
            if (prompt.isChange) Res.string.connection_certificate_title_changed else Res.string.connection_certificate_title_untrusted,
        ),
        borderColor = accent,
        modifier = modifier.testTag(TestTags.ConnectionCertificatePrompt),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd)) {
            Text(
                text = stringResource(
                    if (prompt.isChange) Res.string.connection_certificate_body_changed else Res.string.connection_certificate_body_untrusted,
                ),
                style = AppTheme.typography.bodySm,
                color = if (prompt.isChange) accent else AppTheme.colors.textSecondary,
            )
            if (prompt.certificate.isSelfSigned) {
                TagChip(text = stringResource(Res.string.connection_certificate_self_signed))
            }
            CertificateDetail(
                label = stringResource(Res.string.connection_certificate_fingerprint),
                value = ServerCertificateUi.wrappableFingerprint(prompt.certificate.fingerprint),
                isCode = true,
                modifier = Modifier.testTag(TestTags.ConnectionCertificateFingerprint),
            )
            CertificateDetail(stringResource(Res.string.connection_certificate_subject), prompt.certificate.subject)
            if (!prompt.certificate.isSelfSigned) {
                CertificateDetail(stringResource(Res.string.connection_certificate_issuer), prompt.certificate.issuer)
            }
            CertificateDetail(stringResource(Res.string.connection_certificate_valid_until), prompt.certificate.validUntil)
            val trustText = stringResource(
                if (prompt.isChange) Res.string.connection_certificate_trust_new else Res.string.connection_certificate_trust,
            )
            val trustModifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .testTag(TestTags.ConnectionCertificateTrust)
            if (prompt.isChange) {
                PrimaryButton(
                    text = trustText,
                    onClick = onTrust,
                    size = ButtonSize.Regular,
                    enabled = enabled,
                    backgroundColor = AppTheme.colors.critical,
                    hoverColor = AppTheme.colors.critical.copy(alpha = HoverAlpha),
                    modifier = trustModifier,
                )
            } else {
                PrimaryButton(
                    text = trustText,
                    onClick = onTrust,
                    size = ButtonSize.Regular,
                    enabled = enabled,
                    modifier = trustModifier,
                )
            }
            SecondaryButton(
                text = stringResource(Res.string.connection_certificate_dismiss),
                onClick = onDismiss,
                size = ButtonSize.Regular,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag(TestTags.ConnectionCertificateDismiss),
            )
        }
    }
}

private const val HoverAlpha = 0.85f

@Composable
private fun CertificateDetail(label: String, value: String, modifier: Modifier = Modifier, isCode: Boolean = false) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
        Text(text = label, style = AppTheme.typography.labelUi, color = AppTheme.colors.textMuted)
        Text(
            text = value,
            style = if (isCode) AppTheme.typography.codeBase else AppTheme.typography.bodySm,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Preview
@Composable
private fun CertificatePromptCardPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            CertificatePromptCard(prompt = PreviewPrompt, enabled = true, onTrust = {}, onDismiss = {})
            CertificatePromptCard(prompt = PreviewPrompt.copy(isChange = true), enabled = true, onTrust = {}, onDismiss = {})
        }
    }
}

private val PreviewPrompt = CertificatePromptUi(
    certificate = ServerCertificateUi(
        fingerprint = "37:B1:BD:A5:AD:60:02:0C:B0:8F:05:67:65:F4:02:F0:AC:0C:90:34:C8:AA:20:42:50:22:79:E3:76:00:CA:E2",
        subject = "CN=nas.local,O=UGREEN",
        issuer = "CN=nas.local,O=UGREEN",
        validUntil = "2027-03-01",
        isSelfSigned = true,
    ),
    isChange = false,
)
