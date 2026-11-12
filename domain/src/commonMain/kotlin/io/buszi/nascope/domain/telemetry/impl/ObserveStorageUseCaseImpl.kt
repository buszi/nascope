package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.MetricRingBuffer
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Data
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.mapValue
import io.buszi.nascope.domain.telemetry.shareWhileObserved
import io.buszi.nascope.domain.telemetry.toTelemetryState
import io.buszi.nascope.domain.telemetry.withHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
internal class ObserveStorageUseCaseImpl(
    private val poller: TelemetryPoller,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveStorageUseCase {

    private class DiskHistory(
        val read: MetricRingBuffer = MetricRingBuffer(),
        val write: MetricRingBuffer = MetricRingBuffer(),
    )

    private class DiskSample(
        val disks: List<DiskIoRate>,
        val readHistory: List<Float>,
        val writeHistory: List<Float>,
    )

    private val state: Flow<TelemetryState<Data>> = poller.fileSystems
        .toTelemetryState(
            poller.diskIo.withHistory(newState = ::DiskHistory, onPolled = ::record),
        ) { fileSystems, sample ->
            Data(
                fileSystems = fileSystems,
                disks = sample?.disks.orEmpty(),
                readBytesPerSec = sample?.disks?.sumOf { it.readBytesPerSec },
                writeBytesPerSec = sample?.disks?.sumOf { it.writeBytesPerSec },
                readHistory = sample?.readHistory.orEmpty(),
                writeHistory = sample?.writeHistory.orEmpty(),
            )
        }
        .shareWhileObserved(applicationScope, dispatcher)

    override fun invoke(): Flow<TelemetryState<Data>> = state

    private fun record(history: DiskHistory, polled: Polled<List<DiskIoRate>>): Polled<DiskSample> {
        when (polled) {
            Polled.Loading -> {
                history.read.clear()
                history.write.clear()
            }
            is Polled.Value -> if (!polled.isStale) {
                history.read.add(polled.value.sumOf { it.readBytesPerSec }.toFloat())
                history.write.add(polled.value.sumOf { it.writeBytesPerSec }.toFloat())
            }
            is Polled.Failed -> Unit
        }
        return polled.mapValue { DiskSample(it, history.read.snapshot(), history.write.snapshot()) }
    }
}
