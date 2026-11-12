package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("io.buszi.nascope.storage")
class StorageModule {

    @Single
    internal fun appDataStore(directory: StorageDirectory): DataStore<Preferences> =
        createAppDataStore(directory.file(APP_DATA_STORE_FILE))

    private companion object {
        const val APP_DATA_STORE_FILE = "nascope.preferences_pb"
    }
}
