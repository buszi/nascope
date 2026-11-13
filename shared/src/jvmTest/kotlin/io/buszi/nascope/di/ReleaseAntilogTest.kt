package io.buszi.nascope.di

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseAntilogTest {

    private class Recorder : Antilog() {
        val logged = mutableListOf<Pair<LogLevel, String?>>()

        override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
            logged += priority to message
        }
    }

    @Test
    fun `drops everything below warning`() {
        val recorder = Recorder()
        val antilog = ReleaseAntilog(recorder)

        LogLevel.entries.forEach { antilog.log(it, "Network", null, it.name) }

        assertEquals(
            listOf(LogLevel.WARNING, LogLevel.ERROR, LogLevel.ASSERT),
            recorder.logged.map { it.first },
        )
    }

    @Test
    fun `does not log request lines written at debug level`() {
        val recorder = Recorder()

        ReleaseAntilog(recorder).log(LogLevel.DEBUG, "Network", null, "REQUEST: http://nas.local:61208/api/4/cpu")

        assertEquals(emptyList(), recorder.logged)
    }
}
