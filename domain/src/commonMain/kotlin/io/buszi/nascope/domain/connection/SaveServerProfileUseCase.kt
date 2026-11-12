package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfile

fun interface SaveServerProfileUseCase {
    suspend operator fun invoke(profile: ServerProfile, password: String?): Result

    /** [secretPersisted] is `false` when the profile's password only lives in memory for this session. */
    data class Result(val secretPersisted: Boolean)
}
