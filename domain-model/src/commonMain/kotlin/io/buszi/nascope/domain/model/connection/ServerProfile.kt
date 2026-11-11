package io.buszi.nascope.domain.model.connection

data class ServerProfile(
    val id: ServerProfileId,
    val alias: String,
    val address: ServerAddress,
    /** Detected by the connection test, never typed by the user. */
    val apiVersion: GlancesApiVersion,
    val auth: ServerAuth,
)
