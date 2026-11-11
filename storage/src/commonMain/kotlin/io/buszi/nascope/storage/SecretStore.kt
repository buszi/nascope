package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId

interface SecretStore {
    /** `false` when secrets only live in memory for this session. */
    val isPersistent: Boolean

    suspend fun put(id: ServerProfileId, secret: String)

    suspend fun get(id: ServerProfileId): String?

    suspend fun remove(id: ServerProfileId)
}
