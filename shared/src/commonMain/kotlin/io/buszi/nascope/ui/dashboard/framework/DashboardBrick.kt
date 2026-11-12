package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Unique identifiers for all modular dashboard telemetry bricks.
 */
enum class DashboardBrickId {
    SystemOverview,
    CpuMatrix,
    Gpu,
    Alerts,
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
 * Modular height of a brick on multi-column grids, expressed in height units (1U, 2U, ...).
 *
 * A brick of N units is `N * unit + (N - 1) * gutter` tall, so a 2U brick lines up exactly
 * with two stacked 1U bricks. Compact (single/two-column mobile) layouts ignore it and wrap content.
 */
@Immutable
sealed interface BrickHeight {

    fun resolve(unit: Dp, gutter: Dp): Dp

    data class Units(val units: Int = 1) : BrickHeight {
        init {
            require(units >= 1) { "Brick height must be at least 1 unit" }
        }

        override fun resolve(unit: Dp, gutter: Dp): Dp = unit * units + gutter * (units - 1)
    }

    companion object {
        val OneUnit: BrickHeight = Units(1)
        val TwoUnits: BrickHeight = Units(2)
    }
}

/**
 * Drill-down content of a brick, rendered in the right-docked inspector pane on Medium/Expanded
 * windows and as a full-screen destination on Compact windows.
 *
 * Every brick with details owns a dedicated implementation (its own screen and ViewModel);
 * details are never shared between bricks.
 */
@Stable
fun interface BrickDetails {

    /**
     * @param onClose Closes the details (pops the details route).
     * @param modifier Layout modifier supplied by the hosting pane / screen.
     */
    @Composable
    fun Content(onClose: () -> Unit, modifier: Modifier)
}

/**
 * Contract implemented by every dashboard brick.
 *
 * Each brick is self-contained: it specifies its sizing policy and renders
 * its own content (which internally creates and binds its dedicated ViewModel).
 *
 * A brick opts into drill-down by providing [details]. Bricks without details are not clickable
 * and show no affordance, so the dashboard only advertises interactions that lead somewhere.
 */
@Stable
interface DashboardBrick {
    val id: DashboardBrickId
    val span: BrickSpan
    val height: BrickHeight get() = BrickHeight.OneUnit
    val details: BrickDetails? get() = null

    /**
     * Glances plugins this brick needs. The brick is shown only while the server offers all of them; empty (the
     * default) means always shown. While the server's plugins are unknown, every brick is shown.
     */
    val requiredPlugins: Set<String> get() = emptySet()

    @Composable
    fun Content(modifier: Modifier)
}
