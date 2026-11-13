package io.buszi.nascope.storage

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmStorageDirectoryTest {

    private fun withHome(block: (home: java.nio.file.Path) -> Unit) {
        val home = Files.createTempDirectory("nascope-home")
        val previous = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        try {
            block(home)
        } finally {
            System.setProperty("user.home", previous)
            home.toFile().deleteRecursively()
        }
    }

    private val isPosix get() = System.getProperty("os.name").orEmpty().let { !it.startsWith("Windows", ignoreCase = true) }

    @Test
    fun theDataDirectoryIsOwnerOnly() = withHome {
        if (!isPosix) return@withHome
        val file = JvmStorageDirectory().file("profiles.pb")

        val directory = file.toFile().parentFile
        assertTrue(directory.isDirectory)
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory.toPath())))
    }

    @Test
    fun anExistingWorldReadableDirectoryIsTightened() = withHome {
        if (!isPosix) return@withHome
        val probe = JvmStorageDirectory().file("probe").toFile().parentFile.toPath()
        Files.setPosixFilePermissions(probe, PosixFilePermissions.fromString("rwxr-xr-x"))

        JvmStorageDirectory().file("profiles.pb")

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(probe)))
    }
}
