package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfileId

fun interface DeleteServerProfileUseCase {
    suspend operator fun invoke(id: ServerProfileId)
}
