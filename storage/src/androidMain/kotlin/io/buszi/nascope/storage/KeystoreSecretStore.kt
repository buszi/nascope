package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.flow.first
import java.util.Base64

/** One Base64 `iv + ciphertext` entry per profile id, in a DataStore file that is excluded from backup. */
internal class KeystoreSecretStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreSecretCipher,
) : SecretStore {

    override val isPersistent: Boolean = true

    override suspend fun put(id: ServerProfileId, secret: String) {
        val encoded = Base64.getEncoder().encodeToString(cipher.encrypt(secret.encodeToByteArray()))
        dataStore.edit { it[stringPreferencesKey(id.value)] = encoded }
    }

    override suspend fun get(id: ServerProfileId): String? {
        val encoded = dataStore.data.first()[stringPreferencesKey(id.value)] ?: return null
        return try {
            cipher.decrypt(Base64.getDecoder().decode(encoded)).decodeToString()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun remove(id: ServerProfileId) {
        dataStore.edit { it.remove(stringPreferencesKey(id.value)) }
    }
}
