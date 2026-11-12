package io.buszi.nascope.domain

import io.buszi.nascope.network.NetworkModule
import io.buszi.nascope.storage.StorageModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    /** Application-level scope for work that outlives any screen, such as shared polling loops. */
    @Single
    internal fun applicationScope(dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
