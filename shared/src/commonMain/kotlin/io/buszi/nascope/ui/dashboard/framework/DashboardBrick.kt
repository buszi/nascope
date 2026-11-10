package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * Unique identifiers for all modular dashboard telemetry bricks.
 */
enum class DashboardBrickId {
    SystemOverview,
    CpuMatrix,
    MemorySwap,
    StoragePools,
    NetworkIo,
    SensorsThermals,
    Containers,
    ProcessList,
}

/**
 * Span rule defining how a brick claims cells within a [LazyGridItemSpanScope].
 *
 * Designed to prevent awkward empty cell gaps at the end of rows:
 * - [Single]: Takes 1 cell. If only 1 cell remains in the row, it fits perfectly.
 * - [Double]: Prefers 2 cells. If only 1 cell remains in the row, takes 1 cell to fill the gap.
 * - [FullRow]: Always occupies the remainder of the current line (`maxCurrentLineSpan`).
 * - [Custom]: Custom logic given `maxLineSpan` and `maxCurrentLineSpan`.
 */
sealed interface BrickSpan {

    fun calculateSpan(scope: LazyGridItemSpanScope): GridItemSpan =
        calculateSpan(maxLineSpan = scope.maxLineSpan, maxCurrentLineSpan = scope.maxCurrentLineSpan)

    fun calculateSpan(maxLineSpan: Int, maxCurrentLineSpan: Int): GridItemSpan

    data object Single : BrickSpan {
        override fun calculateSpan(maxLineSpan: Int, maxCurrentLineSpan: Int): GridItemSpan =
            GridItemSpan(1)
    }

    data object Double : BrickSpan {
        override fun calculateSpan(maxLineSpan: Int, maxCurrentLineSpan: Int): GridItemSpan {
            val span = minOf(2, maxCurrentLineSpan)
            return GridItemSpan(maxOf(1, span))
        }
    }

    data object FullRow : BrickSpan {
        override fun calculateSpan(maxLineSpan: Int, maxCurrentLineSpan: Int): GridItemSpan =
            GridItemSpan(maxCurrentLineSpan)
    }

    data class Adaptive(
        val compactSpan: Int = 1,
        val expandedSpan: Int = 2,
    ) : BrickSpan {
        override fun calculateSpan(maxLineSpan: Int, maxCurrentLineSpan: Int): GridItemSpan {
            val target = if (maxLineSpan <= 2) compactSpan else expandedSpan
            val actual = minOf(target, maxCurrentLineSpan)
            return GridItemSpan(maxOf(1, actual))
        }
    }
}

/**
 * Contract implemented by every dashboard brick.
 *
 * Each brick is self-contained: it specifies its sizing policy and renders
 * its own content (which internally creates and binds its dedicated ViewModel).
 */
@Stable
interface DashboardBrick {
    val id: DashboardBrickId
    val span: BrickSpan

    @Composable
    fun Content(modifier: Modifier)
}
