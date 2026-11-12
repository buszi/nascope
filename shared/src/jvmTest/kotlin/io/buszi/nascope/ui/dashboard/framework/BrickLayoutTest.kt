package io.buszi.nascope.ui.dashboard.framework

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class BrickLayoutTest {

    private val doubles = BrickSpan.Double
    private val full = BrickSpan.FullRow

    /** Places [spans] the way a grid does and fails on a hole: a brick that does not fit the rest of its row. */
    private fun assertNoGaps(spans: List<Int>, columns: Int, context: String) {
        var cursor = 0
        for (span in spans) {
            assertTrue(span in 1..columns, "$context: span $span outside 1..$columns in $spans")
            if (cursor + span > columns) fail("$context: hole before a span of $span in $spans")
            cursor = (cursor + span) % columns
        }
        assertEquals(0, cursor, "$context: the last row is unfinished in $spans")
    }

    @Test
    fun noBricksNeedNoSpans() {
        assertEquals(emptyList(), computeBrickSpans(emptyList(), 3))
    }

    @Test
    fun twoColumnBricksOnTwoColumnsTakeARowEach() {
        assertEquals(listOf(2, 2, 2), computeBrickSpans(List(3) { doubles }, 2))
    }

    @Test
    fun aBrickOnTheLastCellOfARowTakesThatCell() {
        assertEquals(listOf(2, 1, 2, 1), computeBrickSpans(List(4) { doubles }, 3))
    }

    @Test
    fun aLoneBrickInTheLastRowIsWidenedToTheRow() {
        assertEquals(listOf(2, 2, 4), computeBrickSpans(List(3) { doubles }, 4))
        assertEquals(listOf(2, 1, 3), computeBrickSpans(List(3) { doubles }, 3))
    }

    @Test
    fun fullRowIsARowOfItsOwnAndCompletesTheRowBeforeIt() {
        // Seven two-column bricks and the process list on three columns: the old rule gave the list one cell.
        assertEquals(listOf(2, 1, 2, 1, 2, 1, 3, 3), computeBrickSpans(List(7) { doubles } + full, 3))
        assertEquals(listOf(2, 2, 2, 2, 4, 4), computeBrickSpans(List(5) { doubles } + full, 4).let { it })
    }

    @Test
    fun aRowCompletedBeforeAFullRowSpreadsTheExtraCells() {
        assertEquals(listOf(2, 2, 4), computeBrickSpans(listOf(BrickSpan.Single, BrickSpan.Single, full), 4))
    }

    @Test
    fun adaptiveBricksFollowTheColumnCount() {
        val adaptive = BrickSpan.Adaptive(compactSpan = 1, expandedSpan = 2)
        assertEquals(listOf(1, 1), computeBrickSpans(listOf(adaptive, adaptive), 2))
        assertEquals(listOf(2, 2), computeBrickSpans(listOf(adaptive, adaptive), 4))
    }

    @Test
    fun theRegisteredBricksLeaveNoHoleInAnySubsetAtTwoThreeAndFourColumns() {
        val bricks = BrickRegistry().getActiveBricks()
        for (columns in 2..4) {
            for (mask in 0 until (1 shl bricks.size)) {
                val subset = bricks.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
                val spans = computeBrickSpans(subset.map { it.span }, columns)
                assertEquals(subset.size, spans.size)
                assertNoGaps(spans, columns, "${subset.map { it.id }} at $columns columns")
                subset.forEachIndexed { i, brick ->
                    if (brick.span == BrickSpan.FullRow) assertEquals(columns, spans[i], "${brick.id} has its own row")
                }
            }
        }
    }

    @Test
    fun everyCombinationOfGenericSpansLeavesNoHole() {
        val kinds = listOf(BrickSpan.Single, doubles, full, BrickSpan.Adaptive(1, 2), BrickSpan.Adaptive(2, 3))
        for (columns in 1..4) {
            fun walk(prefix: List<BrickSpan>) {
                if (prefix.isNotEmpty()) assertNoGaps(computeBrickSpans(prefix, columns), columns, "$prefix")
                if (prefix.size < 5) kinds.forEach { walk(prefix + it) }
            }
            walk(emptyList())
        }
    }
}
