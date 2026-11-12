package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.buszi.nascope.domain.foreground.AppForegroundState
import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Polls each plugin in its own loop; the rules are written once in [poll].
 *
 * - A new [TelemetrySource] (profile switch, demo on/off) cancels the loop and restarts it from `Loading`.
 * - A loop fetches only while the app is in the foreground. Returning after more than [ForgetAfter] restarts it from
 *   `Loading`, so old values are never shown as current.
 * - Failures: stale for the first [FailuresBeforeFailed] - 1 in a row, then `Failed`; `PluginUnavailable` fails at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Single
internal class TelemetryPollerImpl(
    private val sourceProvider: TelemetrySourceProvider,
    private val appForegroundState: AppForegroundState,
    private val observeRefreshInterval: ObserveRefreshIntervalUseCase,
    private val applicationScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : TelemetryPoller {

    private val refreshRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val plugins = poll(TelemetryPlugin.PluginsList) { fetchPlugins() }
    override val system = poll(TelemetryPlugin.System) { fetchSystem() }
    override val uptime = poll(TelemetryPlugin.Uptime) { fetchUptime() }
    override val load = poll(TelemetryPlugin.Load) { fetchLoad() }
    override val cpu = poll(TelemetryPlugin.Cpu) { fetchCpu() }
    override val cpuCores = poll(TelemetryPlugin.PerCpu) { fetchCpuCores() }
    override val cpuInfo = poll(TelemetryPlugin.QuickLook) { fetchCpuInfo() }
    override val memory = poll(TelemetryPlugin.Mem) { fetchMemory() }
    override val swap = poll(TelemetryPlugin.MemSwap) { fetchSwap() }
    override val fileSystems = poll(TelemetryPlugin.Fs) { fetchFileSystems() }
    override val diskIo = poll(TelemetryPlugin.DiskIo) { fetchDiskIo() }
    override val networkInterfaces = poll(TelemetryPlugin.Network) { fetchNetworkInterfaces() }
    override val sensors = poll(TelemetryPlugin.Sensors) { fetchSensors() }
    override val containers = poll(TelemetryPlugin.Containers) { fetchContainers() }
    override val gpus = poll(TelemetryPlugin.Gpu) { fetchGpus() }
    override val alerts = poll(TelemetryPlugin.Alert) { fetchAlerts() }
    override val serverTime = poll(TelemetryPlugin.Now) { fetchServerTime() }
    override val processCount = poll(TelemetryPlugin.ProcessCount) { fetchProcessCount() }
    override val processes = poll(TelemetryPlugin.ProcessList) { fetchProcesses() }
    override val programs = poll(TelemetryPlugin.ProgramList) { fetchPrograms() }

    override fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    private fun <T> poll(
        plugin: TelemetryPlugin,
        fetch: suspend TelemetrySource.() -> Result<T, TelemetryFailure>,
    ): Flow<Polled<T>> = sourceProvider.source
        .flatMapLatest { source ->
            if (source == null) flowOf(Polled.Loading) else loop(plugin, source, fetch)
        }
        .flowOn(dispatcher)
        .shareIn(
            scope = applicationScope,
            // The replayed value is dropped when the loop stops, so a new collector never sees a stale one.
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            replay = 1,
        )

    private fun <T> loop(
        plugin: TelemetryPlugin,
        source: TelemetrySource,
        fetch: suspend TelemetrySource.() -> Result<T, TelemetryFailure>,
    ): Flow<Polled<T>> = channelFlow {
        var current: Polled<T> = Polled.Loading
        var previous: T? = null
        var failures = 0
        var unavailable = false
        var loaded = false

        suspend fun publish(polled: Polled<T>) {
            current = polled
            send(polled)
        }

        publish(Polled.Loading)
        appForegroundState.isForeground.distinctUntilChanged().collectLatest { foreground ->
            if (!foreground) {
                // Cancelled when the app returns in time. Otherwise forget everything: this is a new session.
                delay(ForgetAfter)
                previous = null
                failures = 0
                unavailable = false
                loaded = false
                if (current !is Polled.Loading) publish(Polled.Loading)
                return@collectLatest
            }
            while (true) {
                fetch(source).fold(
                    success = { value ->
                        previous = value
                        failures = 0
                        unavailable = false
                        loaded = true
                        publish(Polled.Value(value, isStale = false))
                    },
                    failure = { failure ->
                        if (failure == TelemetryFailure.PluginUnavailable) {
                            previous = null
                            failures = 0
                            unavailable = true
                            publish(Polled.Failed(failure))
                        } else {
                            failures++
                            val last = previous
                            publish(
                                if (last != null && failures < FailuresBeforeFailed) {
                                    Polled.Value(last, isStale = true)
                                } else {
                                    Polled.Failed(failure)
                                },
                            )
                        }
                    },
                )
                val interval = observeRefreshInterval().first()
                val period = if (unavailable) slowPeriod(interval) else periodOf(plugin.tier, interval, loaded)
                // Wakes up early when refresh() is called.
                withTimeoutOrNull(period) { refreshRequests.first() }
            }
        }
    }

    private fun periodOf(tier: PollTier, interval: RefreshInterval, loaded: Boolean): Duration = when (tier) {
        PollTier.Fast -> interval.seconds.seconds
        PollTier.Slow -> slowPeriod(interval)
        PollTier.Static -> if (loaded) Duration.INFINITE else slowPeriod(interval)
    }

    private fun slowPeriod(interval: RefreshInterval): Duration =
        minOf((interval.seconds * SlowFactor).seconds, MaxSlowPeriod)

    private companion object {
        const val SlowFactor = 5
        val MaxSlowPeriod = 30.seconds
        val ForgetAfter = 30.seconds
        const val FailuresBeforeFailed = 3
    }
}
