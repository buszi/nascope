package io.buszi.nascope.ui.common.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MemorySwapUiTest {

    @Test
    fun usedExcludingCacheTakesTheCacheOutWhenThePartsExceedTheTotal() {
        val memory = MemorySwapUi(totalBytes = 100, usedBytes = 60, cachedBytes = 25, buffersBytes = 5, freeBytes = 20)

        assertEquals(30L, memory.usedExcludingCacheBytes)
    }

    @Test
    fun usedExcludingCacheKeepsUsedWhenThePartsAlreadyFit() {
        val memory = MemorySwapUi(totalBytes = 100, usedBytes = 50, cachedBytes = 30, buffersBytes = 10, freeBytes = 10)

        assertEquals(50L, memory.usedExcludingCacheBytes)
    }

    @Test
    fun usedExcludingCacheIgnoresMissingCacheFigures() {
        val memory = MemorySwapUi(totalBytes = 100, usedBytes = 60, freeBytes = 40)

        assertEquals(60L, memory.usedExcludingCacheBytes)
    }

    @Test
    fun usedExcludingCacheNeverGoesNegative() {
        val memory = MemorySwapUi(totalBytes = 100, usedBytes = 20, cachedBytes = 90, freeBytes = 10)

        assertEquals(0L, memory.usedExcludingCacheBytes)
    }
}
