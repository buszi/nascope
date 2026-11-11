package io.buszi.nascope.domain.model.connection

data class ServerAddress(
    val protocol: ServerProtocol,
    val host: String,
    val port: Int,
)
