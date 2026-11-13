package io.buszi.nascope.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalContentInsets
import io.buszi.nascope.ui.common.util.WindowWidthClass

/**
 * Navigation 3 [SceneStrategy] enabling multi-pane dashboard + detail inspector layouts
 * on tablet and desktop form factors ([WindowWidthClass.Medium] and [WindowWidthClass.Expanded]).
 *
 * Uses a stable [DashboardSplitScene.key] derived from the [mainPane] entry ([NavRoute.Dashboard])
 * whether or not a [detailPane] entry ([NavRoute.Details]) is stacked on top, preventing
 * [NavDisplay] from treating side-panel open/close/switch actions as full-scene transitions
 * that would re-animate or flash the dashboard bricks.
 *
 * On [WindowWidthClass.Compact], returns `null` so Navigation 3 falls back to single-pane
 * full-screen rendering.
 */
@Immutable
class DashboardSplitSceneStrategy<T : Any>(
    private val windowWidthClass: WindowWidthClass,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>,
    ): Scene<T>? {
        if (windowWidthClass == WindowWidthClass.Compact) return null
        if (entries.isEmpty()) return null

        val lastEntry = entries.last()
        if (lastEntry.metadata[MainPaneMetadataKey] == true) {
            return DashboardSplitScene(
                key = lastEntry.contentKey,
                previousEntries = entries.dropLast(1),
                mainEntry = lastEntry,
                detailEntry = null,
            )
        }

        if (entries.size >= 2) {
            val mainEntry = entries[entries.lastIndex - 1]
            val detailEntry = lastEntry
            val isMainPane = mainEntry.metadata[MainPaneMetadataKey] == true
            val isDetailPane = detailEntry.metadata[DetailPaneMetadataKey] == true
            if (isMainPane && isDetailPane) {
                return DashboardSplitScene(
                    key = mainEntry.contentKey,
                    previousEntries = entries.dropLast(1),
                    mainEntry = mainEntry,
                    detailEntry = detailEntry,
                )
            }
        }

        return null
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
 * Adaptive Navigation 3 [Scene] rendering the primary telemetry dashboard and, when present,
 * a right-docked detail inspector pane separated by a hairline border.
 */
@Immutable
data class DashboardSplitScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val mainEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>?,
) : Scene<T> {

    override val entries: List<NavEntry<T>> = listOfNotNull(mainEntry, detailEntry)

    override val metadata: Map<String, Any> =
        (entries.lastOrNull()?.metadata ?: emptyMap()) + SplitSceneNoTransitionMetadata

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
                // With the inspector docked, the dashboard no longer reaches the end edge of the window.
                val contentInsets = LocalContentInsets.current
                val mainInsets = if (detailEntry != null) {
                    contentInsets.exclude(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                } else {
                    contentInsets
                }
                CompositionLocalProvider(LocalContentInsets provides mainInsets) {
                    mainEntry.Content()
                }
            }

            if (detailEntry != null) {
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
}

private val SplitSceneNoTransitionMetadata: Map<String, Any> = metadata {
    put(NavDisplay.TransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
    put(NavDisplay.PopTransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
    put(NavDisplay.PredictivePopTransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
}
