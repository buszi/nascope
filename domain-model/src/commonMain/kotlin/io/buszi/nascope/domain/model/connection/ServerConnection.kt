package io.buszi.nascope.domain.model.connection

/** Everything needed to call one server. Built in :domain from a profile and its stored secret. */
data class ServerConnection(
    val address: ServerAddress,
    val apiVersion: GlancesApiVersion,
    val credentials: ServerCredentials?,
)
