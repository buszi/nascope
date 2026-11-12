package io.buszi.nascope.ui.dashboard.framework

/**
 * The number of cells each brick takes on a grid of [columns], in the order given, so that no row has a hole.
 *
 * - A brick asks for the cells its [BrickSpan] gives on an empty row, and takes at most what is left of the row
 *   (a two-column brick on the last cell of a row takes that one cell).
 * - [BrickSpan.FullRow] is a row of its own: an unfinished row before it is completed first.
 * - A row that ends unfinished (only the last one can) is completed by widening its bricks, starting with the last.
 */
fun computeBrickSpans(spans: List<BrickSpan>, columns: Int): List<Int> {
    require(columns >= 1) { "A grid needs at least one column" }
    val result = MutableList(spans.size) { 1 }
    var row = mutableListOf<Int>() // indices of the bricks in the current row
    var used = 0

    fun closeRow() {
        var free = columns - used
        var i = row.lastIndex
        while (free > 0 && i >= 0) {
            result[row[i]]++
            free--
            i = if (i == 0) row.lastIndex else i - 1
        }
        row = mutableListOf()
        used = 0
    }

    spans.forEachIndexed { index, span ->
        if (span == BrickSpan.FullRow) {
            if (row.isNotEmpty()) closeRow()
            result[index] = columns
            return@forEachIndexed
        }
        val wanted = span.calculateSpan(maxLineSpan = columns, maxCurrentLineSpan = columns).currentLineSpan
        val cells = wanted.coerceIn(1, columns - used)
        result[index] = cells
        row += index
        used += cells
        if (used == columns) {
            row = mutableListOf()
            used = 0
        }
    }
    if (row.isNotEmpty()) closeRow()
    return result
}
