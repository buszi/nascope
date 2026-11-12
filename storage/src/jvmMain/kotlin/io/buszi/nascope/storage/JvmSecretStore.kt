package io.buszi.nascope.storage

import com.github.javakeyring.Keyring
import org.koin.core.annotation.Single

/** OS-keyring secrets, or in-memory when no keyring backend is available. */
@Single(binds = [SecretStore::class])
internal class JvmSecretStore : SecretStore by FallbackSecretStore(createKeyringSecretStore())

private fun createKeyringSecretStore(): SecretStore? = try {
    KeyringSecretStore(Keyring.create())
} catch (_: Exception) {
    null
}
