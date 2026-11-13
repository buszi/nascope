package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nav_connection_connecting
import io.buszi.nascope.resources.nav_connection_offline
import io.buszi.nascope.resources.nav_connection_online
import io.buszi.nascope.resources.nav_connection_password_missing
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import org.jetbrains.compose.resources.StringResource

/** UI representation of the live connection to the active server, as shown in the top bar chip. */
@Immutable
enum class ConnectionStatusUi(
    val labelRes: StringResource,
    val level: StatusLevel,
    val pulsing: Boolean,
) {
    Connecting(Res.string.nav_connection_connecting, StatusLevel.Degraded, pulsing = true),
    Online(Res.string.nav_connection_online, StatusLevel.Operational, pulsing = true),
    Offline(Res.string.nav_connection_offline, StatusLevel.Critical, pulsing = false),

    /** No password is stored on this device for a server that needs one: entering it fixes the connection. */
    PasswordMissing(Res.string.nav_connection_password_missing, StatusLevel.Critical, pulsing = false),
    ;

    companion object {
        /**
         * `Idle` only exists while no server is connected; if it shows up next to a connected session
         * it is the instant before the first check starts, so it reads as [Connecting].
         */
        fun from(data: Status): ConnectionStatusUi = when (data) {
            Status.Idle, Status.Connecting -> Connecting
            Status.Online -> Online
            is Status.Offline -> if (data.failure == ConnectionFailure.PasswordMissing) PasswordMissing else Offline
        }
    }
}
