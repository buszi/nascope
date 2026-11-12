package io.buszi.nascope.domain.telemetry.impl

import app.cash.turbine.test
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveAvailablePluginsUseCaseTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val plugins = MutableStateFlow<Polled<Set<String>>>(Polled.Loading)
    private val gpus = MutableStateFlow<Polled<List<GpuUsage>>>(Polled.Loading)
    private val poller = mockk<TelemetryPoller> {
        every { this@mockk.plugins } returns this@ObserveAvailablePluginsUseCaseTest.plugins
        every { this@mockk.gpus } returns this@ObserveAvailablePluginsUseCaseTest.gpus
    }
    private val useCase = ObserveAvailablePluginsUseCaseImpl(poller)

    private val gpu = GpuUsage("intel0", "Intel UHD", 12f, null, null, null)
    private val offline = TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused)

    @Test
    fun isUnknownWhileLoadingAndWhenTheListFailed() = runTest {
        useCase().test {
            assertEquals(null, awaitItem())
            plugins.value = Polled.Failed(offline)
            runCurrent()
            expectNoEvents() // still null: no new item
        }
    }

    @Test
    fun listsThePluginsOfTheServer() = runTest {
        useCase().test {
            assertEquals(null, awaitItem())
            plugins.value = Polled.Value(setOf("cpu", "mem"), isStale = false)
            assertEquals(setOf("cpu", "mem"), awaitItem())
        }
    }

    @Test
    fun aPluginTheServerDoesNotListIsMissing() = runTest {
        plugins.value = Polled.Value(setOf("cpu"), isStale = false)
        gpus.value = Polled.Value(listOf(gpu), isStale = false)
        useCase().test {
            assertEquals(setOf("cpu"), awaitItem())
        }
    }

    @Test
    fun aListFailureIsUnknownAgainAndARetryThatSucceedsListsThePlugins() = runTest {
        useCase().test {
            assertEquals(null, awaitItem())
            plugins.value = Polled.Failed(offline)
            runCurrent()
            plugins.value = Polled.Value(setOf("cpu"), isStale = false)
            assertEquals(setOf("cpu"), awaitItem())
        }
    }

    @Test
    fun aConfigDependentPluginWithAnEmptyFirstAnswerIsHiddenForTheSession() = runTest {
        plugins.value = Polled.Value(setOf("cpu", "gpu"), isStale = false)
        gpus.value = Polled.Value(emptyList(), isStale = false)
        useCase().test {
            assertEquals(setOf("cpu"), awaitItem())
            // The decision is made: a later non-empty answer is not even looked at.
            gpus.value = Polled.Value(listOf(gpu), isStale = false)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun aConfigDependentPluginIsHiddenUntilItsFirstSuccessfulPollAndThenStaysVisible() = runTest {
        plugins.value = Polled.Value(setOf("cpu", "gpu"), isStale = false)
        useCase().test {
            assertEquals(setOf("cpu"), awaitItem()) // loading
            gpus.value = Polled.Failed(offline)
            runCurrent()
            gpus.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()
            gpus.value = Polled.Value(listOf(gpu), isStale = false)
            assertEquals(setOf("cpu", "gpu"), awaitItem())
            // Later polls fail or come back empty: the brick does not flap.
            gpus.value = Polled.Value(listOf(gpu), isStale = true)
            gpus.value = Polled.Failed(offline)
            gpus.value = Polled.Value(emptyList(), isStale = false)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun aNewSessionDecidesAgain() = runTest {
        plugins.value = Polled.Value(setOf("gpu"), isStale = false)
        gpus.value = Polled.Value(listOf(gpu), isStale = false)
        useCase().test {
            assertEquals(setOf("gpu"), awaitItem())
            // Switching servers: the list is reloaded, then answers again.
            plugins.value = Polled.Loading
            assertEquals(null, awaitItem())
            gpus.value = Polled.Value(emptyList(), isStale = false)
            plugins.value = Polled.Value(setOf("gpu", "cpu"), isStale = false)
            assertEquals(setOf("cpu"), awaitItem())
        }
    }

    @Test
    fun theStalePollOfAFailingFirstAnswerDoesNotDecide() = runTest {
        plugins.value = Polled.Value(setOf("gpu"), isStale = false)
        useCase().test {
            assertEquals(emptySet(), awaitItem())
            gpus.value = Polled.Value(emptyList(), isStale = true)
            runCurrent()
            gpus.value = Polled.Value(listOf(gpu), isStale = false)
            assertTrue("gpu" in awaitItem()!!)
        }
    }
}
