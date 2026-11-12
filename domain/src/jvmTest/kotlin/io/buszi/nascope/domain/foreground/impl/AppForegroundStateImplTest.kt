package io.buszi.nascope.domain.foreground.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AppForegroundStateImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.dispatcher)
    private val state by lazy { AppForegroundStateImpl(applicationScope) }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun appForegroundStateStartsInTheBackground() = runTest {
        turbineScope {
            val turbine = state.isForeground.testIn(this)
            runCurrent()

            assertEquals(listOf(false), turbine.cancelAndConsumeItems())
        }
    }

    @Test
    fun appForegroundStateReportsForegroundImmediately() = runTest {
        turbineScope {
            val turbine = state.isForeground.testIn(this)
            runCurrent()
            state.set(true)
            runCurrent()

            assertEquals(listOf(false, true), turbine.cancelAndConsumeItems())
        }
    }

    @Test
    fun appForegroundStateNeverReportsABackgroundThatEndsWithinTheGracePeriod() = runTest {
        turbineScope {
            val turbine = state.isForeground.testIn(this)
            state.set(true)
            runCurrent()
            state.set(false)
            runCurrent()
            advanceTimeBy(4_999.milliseconds)
            runCurrent()
            state.set(true)
            runCurrent()
            advanceTimeBy(60_000.milliseconds)
            runCurrent()

            assertEquals(listOf(false, true), turbine.cancelAndConsumeItems())
        }
    }

    @Test
    fun appForegroundStateReportsBackgroundAfterTheGracePeriod() = runTest {
        turbineScope {
            val turbine = state.isForeground.testIn(this)
            state.set(true)
            runCurrent()
            state.set(false)
            runCurrent()
            advanceTimeBy(4_999.milliseconds)
            runCurrent()
            assertEquals(listOf(false, true), turbine.cancelAndConsumeItems())

            val later = state.isForeground.testIn(this)
            advanceTimeBy(1.milliseconds)
            runCurrent()

            assertEquals(listOf(true, false), later.cancelAndConsumeItems())
        }
    }
}
