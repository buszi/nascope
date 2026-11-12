package io.buszi.nascope.domain.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals

class MetricRingBufferTest {

    @Test
    fun ringBufferKeepsSamplesInOrderBelowCapacity() {
        val buffer = MetricRingBuffer(capacity = 3)

        buffer.add(1f)
        buffer.add(2f)

        assertEquals(listOf(1f, 2f), buffer.snapshot())
    }

    @Test
    fun ringBufferHoldsExactlyCapacitySamples() {
        val buffer = MetricRingBuffer(capacity = 3)

        listOf(1f, 2f, 3f).forEach(buffer::add)

        assertEquals(listOf(1f, 2f, 3f), buffer.snapshot())
    }

    @Test
    fun ringBufferDropsTheOldestSamplesInOrderWhenItWrapsAround() {
        val buffer = MetricRingBuffer(capacity = 3)

        (1..8).forEach { buffer.add(it.toFloat()) }

        assertEquals(listOf(6f, 7f, 8f), buffer.snapshot())
        assertEquals(3, buffer.size)
    }

    @Test
    fun ringBufferSnapshotIsNotChangedByLaterAppends() {
        val buffer = MetricRingBuffer(capacity = 2)
        buffer.add(1f)
        val snapshot = buffer.snapshot()

        buffer.add(2f)

        assertEquals(listOf(1f), snapshot)
    }

    @Test
    fun ringBufferStartsOverAfterClear() {
        val buffer = MetricRingBuffer(capacity = 3)
        (1..5).forEach { buffer.add(it.toFloat()) }

        buffer.clear()
        buffer.add(9f)

        assertEquals(listOf(9f), buffer.snapshot())
    }

    @Test
    fun ringBufferDefaultsToSixtySamples() {
        val buffer = MetricRingBuffer()

        repeat(61) { buffer.add(it.toFloat()) }

        assertEquals(60, buffer.snapshot().size)
        assertEquals(1f, buffer.snapshot().first())
    }

    @Test
    fun keyedHistoryKeepsOneBufferPerKeyAndDropsVanishedKeys() {
        val history = KeyedMetricHistory<String>()

        history.add("a", 1f)
        history.add("b", 10f)
        history.add("a", 2f)
        history.retainOnly(setOf("a"))

        assertEquals(listOf(1f, 2f), history.snapshot("a"))
        assertEquals(emptyList(), history.snapshot("b"))
        assertEquals(listOf(5f), history.add("b", 5f))
    }
}
