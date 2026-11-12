package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import kotlinx.coroutines.flow.Flow

fun interface ObserveConnectionStatusUseCase {
    operator fun invoke(): Flow<Status>

    sealed interface Status {
        /** No server is connected (nothing configured, or demo mode). */
        data object Idle : Status

        /** A server is connected and the first check is running. */
        data object Connecting : Status

        data object Online : Status

        data class Offline(val failure: ConnectionFailure) : Status
    }
}
