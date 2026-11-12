package io.buszi.nascope.domain

import io.buszi.nascope.network.NetworkModule
import io.buszi.nascope.storage.StorageModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [NetworkModule::class, StorageModule::class])
@Configuration
@ComponentScan("io.buszi.nascope.domain")
class DomainModule {

    /** The dispatcher use cases switch to for their work; tests pass a test dispatcher instead. */
    @Single
    internal fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
