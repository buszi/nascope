package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.runtime.Immutable
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
     */
    data class Offline(
        val reason: Message = Message.plain("Host unreachable"),
        val onReconnect: (() -> Unit)? = null,
    ) : WidgetState {
        constructor(reason: String, onReconnect: (() -> Unit)? = null) : this(
            reason = Message.plain(reason),
            onReconnect = onReconnect,
        )
    }
}
