package io.buszi.nascope.domain.telemetry

/** Fixed-capacity history of one metric: O(1) append that allocates nothing, oldest sample dropped when full. */
internal class MetricRingBuffer(val capacity: Int = DefaultCapacity) {

    private val values = FloatArray(capacity)
    private var start = 0

    var size: Int = 0
        private set

    fun add(value: Float) {
        if (size < capacity) {
            values[(start + size) % capacity] = value
            size++
        } else {
            values[start] = value
            start = (start + 1) % capacity
        }
    }

    fun clear() {
        start = 0
        size = 0
    }

    /** The samples, oldest first. A copy, so later appends do not change it. */
    fun snapshot(): List<Float> = List(size) { values[(start + it) % capacity] }

    companion object {
        const val DefaultCapacity = 60
    }
}

/** One [MetricRingBuffer] per key (interface name, sensor label); a key that vanishes loses its buffer. */
internal class KeyedMetricHistory<K> {

    private val buffers = mutableMapOf<K, MetricRingBuffer>()

    /** Appends [value] for [key] and returns the key's snapshot. */
    fun add(key: K, value: Float): List<Float> {
        val buffer = buffers.getOrPut(key) { MetricRingBuffer() }
        buffer.add(value)
        return buffer.snapshot()
    }

    /** The current samples of [key], oldest first; empty for an unknown key. */
    fun snapshot(key: K): List<Float> = buffers[key]?.snapshot().orEmpty()

    /** Drops the buffers of every key not in [present]. */
    fun retainOnly(present: Set<K>) {
        buffers.keys.retainAll(present)
    }

    fun clear() = buffers.clear()
}
