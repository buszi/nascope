package io.buszi.nascope.storage

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OS keyring: service `io.buszi.nascope`, account = profile id. */
internal class KeyringSecretStore(private val keyring: Keyring) : SecretStore {

    override val isPersistent: Boolean = true

    override suspend fun put(id: ServerProfileId, secret: String) {
        withContext(Dispatchers.IO) { keyring.setPassword(SERVICE, id.value, secret) }
    }

    override suspend fun get(id: ServerProfileId): String? = withContext(Dispatchers.IO) {
        try {
            keyring.getPassword(SERVICE, id.value)
        } catch (_: PasswordAccessException) {
            null
        }
    }

    override suspend fun remove(id: ServerProfileId) {
        withContext(Dispatchers.IO) {
            try {
                keyring.deletePassword(SERVICE, id.value)
            } catch (_: PasswordAccessException) {
                // Nothing stored for this id.
            }
        }
    }

    private companion object {
        const val SERVICE = "io.buszi.nascope"
    }
}
