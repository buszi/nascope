package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.foundation.lazy.grid.GridItemSpan
import kotlin.test.Test
import kotlin.test.assertEquals

class BrickSpanTest {

    @Test
    fun singleSpanAlwaysReturnsOne() {
        assertEquals(GridItemSpan(1), BrickSpan.Single.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 4))
        assertEquals(GridItemSpan(1), BrickSpan.Single.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 1))
    }

    @Test
    fun doubleSpanFillsTwoCellsOrClampsToOne() {
        assertEquals(GridItemSpan(2), BrickSpan.Double.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 4))
        assertEquals(GridItemSpan(1), BrickSpan.Double.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 1))
    }

    @Test
    fun fullRowSpanOccupiesMaxCurrentLineSpan() {
        assertEquals(GridItemSpan(4), BrickSpan.FullRow.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 4))
        assertEquals(GridItemSpan(2), BrickSpan.FullRow.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 2))
    }

    @Test
    fun adaptiveSpanAdaptsToMaxLineSpanAndRemainingCells() {
        val adaptive = BrickSpan.Adaptive(compactSpan = 1, expandedSpan = 2)

        // Mobile (2 columns): compact span (1)
        assertEquals(GridItemSpan(1), adaptive.calculateSpan(maxLineSpan = 2, maxCurrentLineSpan = 2))

        // Tablet/Desktop (3 or 4 columns) with enough space: expanded span (2)
        assertEquals(GridItemSpan(2), adaptive.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 4))

        // Desktop with only 1 cell remaining in row: clamps to 1 to prevent gap
        assertEquals(GridItemSpan(1), adaptive.calculateSpan(maxLineSpan = 4, maxCurrentLineSpan = 1))
    }
}
