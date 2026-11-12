package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfileId

fun interface SetActiveServerUseCase {
    suspend operator fun invoke(id: ServerProfileId)
}
