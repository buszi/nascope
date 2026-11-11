package io.buszi.nascope.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import okio.Path.Companion.toPath
import java.io.File

/** One DataStore on a file; [close] releases it so the same file can be opened again. */
internal class DataStoreTestFile(private val file: File) {
    private val job = Job()

    val dataStore: DataStore<Preferences> = createAppDataStore(
        path = file.absolutePath.toPath(),
        scope = CoroutineScope(Dispatchers.IO + job),
    )

    suspend fun close() = job.cancelAndJoin()
}
