package io.buszi.nascope.storage

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import java.io.File

/** The `Context` is registered at runtime by `NascopeApplication`, so it is not visible to compile-time checks. */
@Single
internal class AndroidStorageDirectory(@Provided private val context: Context) : StorageDirectory {
    override fun file(name: String): Path {
        val file = File(context.filesDir, "datastore/$name")
        file.parentFile?.mkdirs()
        return file.absolutePath.toPath()
    }
}
