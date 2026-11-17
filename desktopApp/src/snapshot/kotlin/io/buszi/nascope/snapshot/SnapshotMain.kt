package io.buszi.nascope.snapshot

import io.buszi.nascope.di.initNascope
import io.buszi.nascope.storage.SecretStore
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.File
import kotlin.system.exitProcess

/**
 * Entry point of `./gradlew :desktopApp:snapshot` (see `docs/desktop-snapshots.md`).
 *
 * Renders the real app offscreen, driven by a [SnapshotScript], against a scratch home directory: the user's own
 * settings, server profiles and keychain are never read or written.
 */
fun main(args: Array<String>) {
    val options = try {
        SnapshotOptions.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println("snapshot: ${e.message}")
        System.err.println(SnapshotOptions.Usage)
        exitProcess(2)
    }
    val commands = try {
        SnapshotScript.parse(options.script.readText())
    } catch (e: IllegalArgumentException) {
        System.err.println("snapshot: ${options.script}: ${e.message}")
        exitProcess(2)
    }

    options.out.mkdirs()
    val home = isolateHome(options)
    println("snapshot: home ${home.path}")
    println("snapshot: out  ${options.out.path}")

    initNascope(isDebug = true)
    // The desktop secret store is the OS keychain; snapshots keep secrets in memory only.
    loadKoinModules(module { single<SecretStore> { InMemorySnapshotSecretStore() } })

    val passed = SnapshotRunner(out = options.out, scale = options.density).run(commands)
    // DataStore and the polling loops keep threads alive; the run is over.
    exitProcess(if (passed) 0 else 1)
}

/**
 * Points `user.home` at a scratch directory before anything reads it, so the app's storage (`JvmStorageDirectory`)
 * lives there. Without `--home`, a fresh `<out>/home` is used (wiped first); with it, the directory is kept, so a
 * later run sees what an earlier one stored (an "app restart").
 */
private fun isolateHome(options: SnapshotOptions): File {
    val realHome = File(System.getProperty("user.home")).canonicalFile
    val home = (options.home ?: File(options.out, "home").also { it.deleteRecursively() })
        .apply { mkdirs() }
        .canonicalFile
    require(home != realHome) { "refusing to run against the real home directory $realHome" }

    // Skiko unpacks its native library under the home directory; keep using the real cache instead of a copy per run.
    if (System.getProperty("skiko.data.path") == null) {
        System.setProperty("skiko.data.path", File(realHome, ".skiko").path)
    }
    System.setProperty("user.home", home.path)
    return home
}

internal class SnapshotOptions(
    val script: File,
    val out: File,
    val home: File?,
    val density: Float,
) {
    companion object {
        const val Usage =
            "usage: --script <file> --out <dir> [--home <dir>] [--density <n>]"

        fun parse(args: Array<String>): SnapshotOptions {
            val values = args.toList().chunked(2).associate { pair ->
                require(pair.size == 2 && pair[0].startsWith("--")) { "unexpected arguments: ${args.joinToString(" ")}" }
                pair[0].removePrefix("--") to pair[1]
            }
            val unknown = values.keys - setOf("script", "out", "home", "density")
            require(unknown.isEmpty()) { "unknown options: $unknown" }
            val script = File(requireNotNull(values["script"]) { "--script is required" })
            require(script.isFile) { "no script at $script" }
            return SnapshotOptions(
                script = script,
                out = File(requireNotNull(values["out"]) { "--out is required" }),
                home = values["home"]?.let(::File),
                density = values["density"]?.toFloatOrNull()?.takeIf { it > 0f }
                    ?: if (values["density"] == null) 1f else throw IllegalArgumentException("--density must be > 0"),
            )
        }
    }
}
