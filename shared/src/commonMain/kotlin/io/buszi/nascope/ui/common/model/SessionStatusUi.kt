package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session

/** What the data source behind the dashboard is, for the indicators that follow the live connection. */
@Immutable
sealed interface SessionStatusUi {

    /** No data source yet: no indicator. */
    data object None : SessionStatusUi

    /** Demo mode: a neutral `DEMO` indicator instead of a connection status. */
    data object Demo : SessionStatusUi

    /** A real server and the live state of its connection. */
    data class Server(val connection: ConnectionStatusUi) : SessionStatusUi

    companion object {
        fun from(session: Session, status: Status): SessionStatusUi = when (session) {
            Session.NotConfigured -> None
            Session.Demo -> Demo
            is Session.Connected -> Server(ConnectionStatusUi.from(status))
        }
    }
}
