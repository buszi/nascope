package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.flow.Flow

interface ServerProfileStore {
    val profiles: Flow<List<ServerProfile>>
    val activeProfileId: Flow<ServerProfileId?>

    suspend fun upsert(profile: ServerProfile)

    suspend fun remove(id: ServerProfileId)

    suspend fun setActiveProfileId(id: ServerProfileId?)
}
