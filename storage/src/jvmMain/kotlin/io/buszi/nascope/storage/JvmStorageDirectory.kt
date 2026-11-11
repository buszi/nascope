package io.buszi.nascope.storage

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import java.io.File

@Single
internal class JvmStorageDirectory : StorageDirectory {
    private val root: File = resolveRoot()

    override fun file(name: String): Path {
        root.mkdirs()
        return File(root, name).absolutePath.toPath()
    }

    private fun resolveRoot(): File {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val home = System.getProperty("user.home").orEmpty()
        return when {
            "mac" in osName -> File(home, "Library/Application Support/nascope")
            "win" in osName -> File(System.getenv("APPDATA") ?: File(home, "AppData/Roaming").path, "nascope")
            else -> File(System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: File(home, ".config").path, "nascope")
        }
    }
}
