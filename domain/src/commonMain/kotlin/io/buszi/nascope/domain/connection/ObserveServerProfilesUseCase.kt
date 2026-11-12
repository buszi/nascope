package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfile
import kotlinx.coroutines.flow.Flow

fun interface ObserveServerProfilesUseCase {
    operator fun invoke(): Flow<List<ServerProfile>>
}
