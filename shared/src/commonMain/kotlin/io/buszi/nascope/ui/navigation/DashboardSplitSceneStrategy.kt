package io.buszi.nascope.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass

/**
 * Navigation 3 [SceneStrategy] enabling multi-pane dashboard + detail inspector layouts
 * on expanded form factors ([WindowWidthClass.Expanded]).
 *
 * When the active window width is [WindowWidthClass.Expanded] and the top two entries on the
 * backstack are a [mainPane] entry ([NavRoute.Dashboard]) followed by a [detailPane] entry
 * ([NavRoute.Details]), both entries are rendered side-by-side within a single [DashboardSplitScene].
 * On [WindowWidthClass.Compact] and [WindowWidthClass.Medium], returns `null` so Navigation 3
 * falls back to single-pane full-screen rendering.
 */
@Immutable
class DashboardSplitSceneStrategy<T : Any>(
    private val windowWidthClass: WindowWidthClass,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>,
    ): Scene<T>? {
        if (windowWidthClass != WindowWidthClass.Expanded) return null
        if (entries.size < 2) return null

        val mainEntry = entries[entries.lastIndex - 1]
        val detailEntry = entries.last()

        val isMainPane = mainEntry.metadata[MainPaneMetadataKey] == true
        val isDetailPane = detailEntry.metadata[DetailPaneMetadataKey] == true
        if (!isMainPane || !isDetailPane) return null

        return DashboardSplitScene(
            key = Pair(mainEntry.contentKey, detailEntry.contentKey),
            previousEntries = entries.dropLast(1),
            mainEntry = mainEntry,
            detailEntry = detailEntry,
        )
    }

    companion object {
        private const val MainPaneMetadataKey = "io.buszi.nascope.ui.navigation.MainPane"
        private const val DetailPaneMetadataKey = "io.buszi.nascope.ui.navigation.DetailPane"

        /** Marks a [NavEntry] as the primary dashboard workspace pane for split-scene composition. */
        fun mainPane(): Map<String, Any> = mapOf(MainPaneMetadataKey to true)

        /** Marks a [NavEntry] as the contextual right-docked detail inspector pane. */
        fun detailPane(): Map<String, Any> = mapOf(DetailPaneMetadataKey to true)
    }
}

/**
 * Two-pane Navigation 3 [Scene] rendering the primary telemetry dashboard alongside
 * a right-docked detail inspector pane separated by a hairline border.
 */
@Immutable
data class DashboardSplitScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val mainEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {

    override val entries: List<NavEntry<T>> = listOf(mainEntry, detailEntry)

    override val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.canvasBackground),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                mainEntry.Content()
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(AppTheme.dimensions.hairlineBorder)
                    .background(AppTheme.colors.baseBorder),
            )

            Box(
                modifier = Modifier
                    .width(AppTheme.dimensions.detailInspectorWidth)
                    .fillMaxHeight()
                    .background(AppTheme.colors.subSurface),
            ) {
                detailEntry.Content()
            }
        }
    }
}
