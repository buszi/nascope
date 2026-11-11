package io.buszi.nascope.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory

internal const val SECRETS_FILE = "secrets.preferences_pb"

internal actual fun createPlatformSecretStore(directory: StorageDirectory): SecretStore? = try {
    val cipher = KeystoreSecretCipher()
    cipher.ensureKey()
    KeystoreSecretStore(
        dataStore = PreferenceDataStoreFactory.createWithPath(produceFile = { directory.file(SECRETS_FILE) }),
        cipher = cipher,
    )
} catch (_: Exception) {
    null
}
