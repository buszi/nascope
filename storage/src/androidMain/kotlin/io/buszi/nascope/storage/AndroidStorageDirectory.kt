package io.buszi.nascope.storage

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import java.io.File

@Single
internal class AndroidStorageDirectory(private val context: Context) : StorageDirectory {
    override fun file(name: String): Path {
        val file = File(context.filesDir, "datastore/$name")
        file.parentFile?.mkdirs()
        return file.absolutePath.toPath()
    }
}
