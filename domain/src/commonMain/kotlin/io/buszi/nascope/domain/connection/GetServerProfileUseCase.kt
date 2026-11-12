package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId

fun interface GetServerProfileUseCase {
    suspend operator fun invoke(id: ServerProfileId): ServerProfile?
}
