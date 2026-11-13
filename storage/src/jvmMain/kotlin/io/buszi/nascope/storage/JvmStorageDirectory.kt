package io.buszi.nascope.storage

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

@Single
internal class JvmStorageDirectory : StorageDirectory {
    private val root: File = resolveRoot()

    override fun file(name: String): Path {
        root.mkdirs()
        restrictToOwner(root)
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

    private companion object {
        /**
         * The profile list (hosts, ports, user names) is for its owner only: on macOS and Linux other local users
         * could read a `0755` directory. Windows already keeps `%APPDATA%` private to the user (no POSIX view there).
         */
        fun restrictToOwner(directory: File) {
            try {
                Files.setPosixFilePermissions(directory.toPath(), PosixFilePermissions.fromString("rwx------"))
            } catch (_: UnsupportedOperationException) {
                // Not a POSIX file system.
            } catch (_: java.io.IOException) {
                // Not ours to change (e.g. a managed directory): keep working with what is there.
            }
        }
    }
}
