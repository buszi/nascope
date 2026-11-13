package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerProfile

/**
 * Whether [ServerProfile] uses a password but none is stored on this device (restored or moved profile, cleared
 * keyring). A stored empty password does not count as missing.
 */
fun interface IsPasswordMissingUseCase {
    suspend operator fun invoke(profile: ServerProfile): Boolean
}
