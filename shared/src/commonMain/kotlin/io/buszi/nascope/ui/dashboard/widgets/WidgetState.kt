package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_state_invalid_response
import io.buszi.nascope.ui.common.model.ConnectionFailureUi
import io.buszi.nascope.ui.common.util.Message

/**
 * Represents the lifecycle and readiness state of a telemetry dashboard widget.
 */
@Immutable
sealed interface WidgetState {

    val isReady: Boolean
        get() = false

    /**
     * Telemetry data is being actively retrieved or initialized.
     */
    data object Loading : WidgetState

    /**
     * Telemetry is live, operational, and active.
     */
    data object Ready : WidgetState {
        override val isReady: Boolean = true
    }

    /**
     * Content is rendered, but the latest poll failed: the values are from an earlier one.
     * The widget header shows a stale tag.
     */
    data object Stale : WidgetState {
        override val isReady: Boolean = true
    }

    /**
     * The host does not provide this telemetry (plugin missing or disabled). Not an error: no retry.
     */
    data object Unavailable : WidgetState

    /**
     * Failed to fetch or deserialize telemetry data from the host.
     *
     * @param message Structured message describing the failure condition.
     * @param onRetry Optional callback to re-trigger the telemetry fetch.
     */
    data class Error(
        val message: Message,
        val onRetry: (() -> Unit)? = null,
    ) : WidgetState {
        constructor(message: String, onRetry: (() -> Unit)? = null) : this(
            message = Message.plain(message),
            onRetry = onRetry,
        )
    }

    /**
     * The host daemon or telemetry subsystem is unreachable / offline.
     *
     * @param reason Structured message explaining the offline condition.
     * @param onReconnect Optional callback to attempt reconnecting.
     * @param action What the widget offers: retrying ([onReconnect]), or fixing the server's profile in the editor.
     */
    data class Offline(
        val reason: Message = Message.plain("Host unreachable"),
        val onReconnect: (() -> Unit)? = null,
        val action: OfflineActionUi = OfflineActionUi.Reconnect,
    ) : WidgetState {
        constructor(reason: String, onReconnect: (() -> Unit)? = null) : this(
            reason = Message.plain(reason),
            onReconnect = onReconnect,
        )
    }
}

/**
 * The one mapping from a use case's [TelemetryState] to a [WidgetState], shared by every ViewModel.
 *
 * | [TelemetryState] | [WidgetState] |
 * | :--- | :--- |
 * | `Loading` | `Loading` |
 * | `Ready(isStale = false)` / `Ready(isStale = true)` | `Ready` / `Stale` |
 * | `Failed(PluginUnavailable)` | `Unavailable` |
 * | `Failed(Connection)` | `Offline` with the connection failure message and the action that fixes it |
 * | `Failed(InvalidResponse)` | `Error` |
 *
 * @param onRetry Re-triggers the telemetry fetch from the Offline / Error states.
 */
fun TelemetryState<*>.toWidgetState(onRetry: () -> Unit): WidgetState = when (this) {
    TelemetryState.Loading -> WidgetState.Loading
    is TelemetryState.Ready -> if (isStale) WidgetState.Stale else WidgetState.Ready
    is TelemetryState.Failed -> when (val failure = failure) {
        TelemetryFailure.PluginUnavailable -> WidgetState.Unavailable
        is TelemetryFailure.Connection -> WidgetState.Offline(
            reason = Message.resource(ConnectionFailureUi.from(failure.failure).messageRes),
            onReconnect = onRetry,
            action = OfflineActionUi.from(failure.failure),
        )
        is TelemetryFailure.InvalidResponse -> WidgetState.Error(
            message = Message.resource(Res.string.widget_state_invalid_response),
            onRetry = onRetry,
        )
    }
}

/**
 * What an offline widget offers. Retrying cannot fix a missing password or an untrusted certificate: those are
 * fixed in the active server's editor, opened through [LocalOpenActiveServerEditor].
 */
enum class OfflineActionUi {
    Reconnect,
    EnterPassword,
    ReviewCertificate,
    ;

    companion object {
        fun from(data: ConnectionFailure): OfflineActionUi = when (data) {
            ConnectionFailure.PasswordMissing -> EnterPassword
            is ConnectionFailure.UntrustedCertificate, is ConnectionFailure.CertificateChanged -> ReviewCertificate
            else -> Reconnect
        }
    }
}

/**
 * Opens the connection editor for the active server; provided by the dashboard, `null` where there is no
 * server to edit (demo mode, previews), in which case the editor actions are not offered.
 */
val LocalOpenActiveServerEditor = compositionLocalOf<(() -> Unit)?> { null }
