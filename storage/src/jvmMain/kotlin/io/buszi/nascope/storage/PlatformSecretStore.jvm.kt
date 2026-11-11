package io.buszi.nascope.storage

import com.github.javakeyring.Keyring

internal actual fun createPlatformSecretStore(directory: StorageDirectory): SecretStore? = try {
    KeyringSecretStore(Keyring.create())
} catch (_: Exception) {
    null
}
