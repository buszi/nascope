package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfile
import kotlinx.coroutines.flow.Flow

fun interface ObserveSessionUseCase {
    operator fun invoke(): Flow<Session>

    sealed interface Session {
        data object NotConfigured : Session
        data object Demo : Session
        data class Connected(val profile: ServerProfile) : Session
    }
}
