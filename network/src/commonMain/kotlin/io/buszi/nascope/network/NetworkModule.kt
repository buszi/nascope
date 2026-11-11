package io.buszi.nascope.network

import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("io.buszi.nascope.network")
class NetworkModule {

    @Single
    internal fun httpClient(): HttpClient = createPlatformHttpClient()
}
