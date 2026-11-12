package io.buszi.nascope.storage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

internal const val SECRETS_FILE = "secrets.preferences_pb"

/**
 * Keystore-backed secrets, or in-memory when the Keystore is unusable.
 *
 * Takes only a public type on purpose: Koin's cross-module checks cannot read definitions whose
 * constructor needs an `internal` type.
 */
@Single(binds = [SecretStore::class])
internal class AndroidSecretStore(
    @Provided context: Context,
) : SecretStore by FallbackSecretStore(createKeystoreSecretStore(context))

private fun createKeystoreSecretStore(context: Context): SecretStore? = try {
    val cipher = KeystoreSecretCipher()
    cipher.ensureKey()
    KeystoreSecretStore(
        dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { AndroidStorageDirectory(context).file(SECRETS_FILE) },
        ),
        cipher = cipher,
    )
} catch (_: Exception) {
    null
}
