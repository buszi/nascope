package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
internal class DataStoreServerProfileStore(
    private val dataStore: DataStore<Preferences>,
) : ServerProfileStore {

    override val profiles: Flow<List<ServerProfile>> = dataStore.data
        .map { decode(it[PROFILES_KEY]).mapNotNull(ServerProfileDto::toDomain) }
        .distinctUntilChanged()

    override val activeProfileId: Flow<ServerProfileId?> = dataStore.data
        .map { preferences -> preferences[ACTIVE_PROFILE_KEY]?.let(::ServerProfileId) }
        .distinctUntilChanged()

    override suspend fun upsert(profile: ServerProfile) {
        val dto = ServerProfileDto.from(profile)
        dataStore.edit { preferences ->
            val stored = decode(preferences[PROFILES_KEY])
            val updated = if (stored.any { it.id == dto.id }) {
                stored.map { if (it.id == dto.id) dto else it }
            } else {
                stored + dto
            }
            preferences[PROFILES_KEY] = encode(updated)
        }
    }

    override suspend fun remove(id: ServerProfileId) {
        dataStore.edit { preferences ->
            preferences[PROFILES_KEY] = encode(decode(preferences[PROFILES_KEY]).filterNot { it.id == id.value })
        }
    }

    override suspend fun setActiveProfileId(id: ServerProfileId?) {
        dataStore.edit { preferences ->
            if (id == null) preferences.remove(ACTIVE_PROFILE_KEY) else preferences[ACTIVE_PROFILE_KEY] = id.value
        }
    }

    private fun decode(json: String?): List<ServerProfileDto> {
        if (json == null) return emptyList()
        return try {
            Json.decodeFromString(serializer, json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private fun encode(profiles: List<ServerProfileDto>): String = Json.encodeToString(serializer, profiles)

    private companion object {
        val PROFILES_KEY = stringPreferencesKey("profiles")
        val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile_id")
        val serializer = ListSerializer(ServerProfileDto.serializer())
    }
}
