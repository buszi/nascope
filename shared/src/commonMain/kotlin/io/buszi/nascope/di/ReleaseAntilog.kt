package io.buszi.nascope.di

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel

/**
 * The logger of release builds: warnings and errors only, so the request lines Ktor logs at debug level
 * (host, path) and any other diagnostic chatter never reach the device log of a user.
 */
internal class ReleaseAntilog(private val delegate: Antilog = DebugAntilog()) : Antilog() {

    override fun isEnable(priority: LogLevel, tag: String?): Boolean = priority >= LogLevel.WARNING

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        delegate.log(priority, tag, throwable, message)
    }
}
