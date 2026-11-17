package io.buszi.nascope.snapshot

import kotlin.system.exitProcess

/**
 * Ends the run when one command takes far longer than it should. Some test-library calls can spin forever on the real
 * app (e.g. `performScrollTo` on the dashboard grid), and a snapshot run must fail, never hang.
 */
internal class CommandWatchdog(private val mainThread: Thread = Thread.currentThread()) {

    @Volatile
    private var deadline: Long = Long.MAX_VALUE

    @Volatile
    private var current: String = ""

    init {
        Thread({
            while (true) {
                Thread.sleep(PollMillis)
                if (System.nanoTime() > deadline) {
                    System.err.println("snapshot: FAILED, $current did not finish in time; it was stuck at:")
                    mainThread.stackTrace.take(StackFrames).forEach { System.err.println("    at $it") }
                    exitProcess(3)
                }
            }
        }, "snapshot-watchdog").apply { isDaemon = true }.start()
    }

    /** Starts the clock for [what]: it may take [expectedMillis] plus a generous margin. */
    fun arm(what: String, expectedMillis: Long) {
        current = what
        deadline = System.nanoTime() + (expectedMillis + MarginMillis) * 1_000_000
    }

    fun disarm() {
        deadline = Long.MAX_VALUE
    }

    private companion object {
        const val PollMillis = 500L
        const val MarginMillis = 60_000L
        const val StackFrames = 25
    }
}
