package io.buszi.nascope.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import org.koin.compose.koinInject
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalContentInsets
import io.buszi.nascope.ui.common.util.LocalWindowWidthClass
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.toWindowWidthClass
import io.buszi.nascope.ui.connection.ConnectionEditorScreen
import io.buszi.nascope.ui.dashboard.DashboardScreen
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.navigation.components.NascopeBottomBar
import io.buszi.nascope.ui.navigation.components.NascopeSidebarRail
import io.buszi.nascope.ui.navigation.components.NascopeTopBar
import io.buszi.nascope.ui.nodes.NodesScreen
import io.buszi.nascope.ui.settings.SettingsScreen

/**
 * Multiplatform adaptive application shell and Navigation 3 host for Nascope.
 *
 * Dynamically adapts its navigation chrome to the ambient window width while preserving
 * the navigation backstack and screen state across window resizes and orientation changes:
 * - [WindowWidthClass.Expanded] (`>= 1280dp`) & [WindowWidthClass.Medium] (`768dp - 1279dp`):
 *   56dp compact icon sidebar rail + top control bar to maximize telemetry canvas space,
 *   plus multi-pane dashboard/inspector split scene via [DashboardSplitSceneStrategy].
 * - [WindowWidthClass.Compact] (`< 768dp`): Bottom navigation bar + top app bar with back navigation.
 *
 * @param modifier Root layout modifier applied to the scaffold container.
 * @param navigator State-driven Navigation 3 controller managing the backstack.
 * @param dashboardContent Composable slot rendering the primary dashboard route.
 */
@Composable
fun NascopeAppScaffold(
    modifier: Modifier = Modifier,
    navigator: NascopeNavigator = rememberNascopeNavigator(),
    dashboardContent: @Composable (
        selectedBrickId: DashboardBrickId?,
        onOpenDetails: (DashboardBrickId) -> Unit,
    ) -> Unit = { selectedBrickId, onOpenDetails ->
        DashboardScreen(selectedBrickId = selectedBrickId, onOpenDetails = onOpenDetails)
    },
) {
    // Keyboard shortcuts (Escape) only reach this scaffold while it is in the focus path. A text field that
    // leaves the composition while focused (e.g. closing the process details after searching) clears focus
    // entirely, so take it back whenever nothing in the scaffold holds focus.
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(hasFocus, navigator.currentRoute) {
        if (!hasFocus) runCatching { focusRequester.requestFocus() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.subSurface)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && navigator.canNavigateBack) {
                    navigator.navigateBack()
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable(),
    ) {
        val windowWidthClass = maxWidth.toWindowWidthClass()
        val isCompact = windowWidthClass == WindowWidthClass.Compact

        // The bars own the insets next to them; the workspace gets the rest and decides itself how to use
        // them, so bricks can scroll behind a navigation bar docked at the side of a rotated phone.
        val safeDrawing = WindowInsets.safeDrawing
        val railInsets = safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
        val topBarInsets = safeDrawing.only(
            if (isCompact) WindowInsetsSides.Top + WindowInsetsSides.Horizontal else WindowInsetsSides.Top + WindowInsetsSides.End,
        )
        val bottomBarInsets = safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        // Compact keeps the bottom bar, which already clears the bottom insets; wide layouts have none.
        val contentInsets = safeDrawing.only(
            if (isCompact) WindowInsetsSides.Horizontal else WindowInsetsSides.End + WindowInsetsSides.Bottom,
        )

        CompositionLocalProvider(
            LocalWindowWidthClass provides windowWidthClass,
            LocalContentInsets provides contentInsets,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.canvasBackground),
            ) {
                if (!isCompact) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(AppTheme.colors.subSurface)
                            .windowInsetsPadding(railInsets),
                    ) {
                        NascopeSidebarRail(
                            activeDestination = navigator.activeTopLevelDestination,
                            onSelectDestination = navigator::navigateToTopLevel,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.colors.subSurface)
                            .windowInsetsPadding(topBarInsets),
                    ) {
                        NascopeTopBar(
                            currentRoute = navigator.currentRoute,
                            canNavigateBack = navigator.canNavigateBack,
                            onNavigateBack = { navigator.navigateBack() },
                            onManageNodes = { navigator.navigateToTopLevel(TopLevelDestination.Nodes) },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(AppTheme.colors.canvasBackground),
                    ) {
                        NascopeNavDisplay(
                            navigator = navigator,
                            windowWidthClass = windowWidthClass,
                            dashboardContent = dashboardContent,
                        )
                    }

                    if (isCompact) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppTheme.colors.subSurface)
                                .windowInsetsPadding(bottomBarInsets),
                        ) {
                            NascopeBottomBar(
                                activeDestination = navigator.activeTopLevelDestination,
                                onSelectDestination = navigator::navigateToTopLevel,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NascopeNavDisplay(
    navigator: NascopeNavigator,
    windowWidthClass: WindowWidthClass,
    dashboardContent: @Composable (
        selectedBrickId: DashboardBrickId?,
        onOpenDetails: (DashboardBrickId) -> Unit,
    ) -> Unit,
) {
    val brickRegistry = koinInject<BrickRegistry>()
    val splitStrategy = remember(windowWidthClass) {
        DashboardSplitSceneStrategy<NavKey>(windowWidthClass = windowWidthClass)
    }
    val singlePaneStrategy = remember {
        SinglePaneSceneStrategy<NavKey>()
    }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { navigator.navigateBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(
            splitStrategy,
            singlePaneStrategy,
        ),
        entryProvider = entryProvider {
            entry<NavRoute.Dashboard>(
                metadata = DashboardSplitSceneStrategy.mainPane(),
            ) {
                dashboardContent(navigator.selectedBrickId, navigator::navigateToDetails)
            }

            entry<NavRoute.Details>(
                metadata = DashboardSplitSceneStrategy.detailPane(),
            ) { route ->
                val details = remember(route.brickId) { brickRegistry.findBrick(route.brickId)?.details }
                if (details != null) {
                    details.Content(
                        onClose = { navigator.navigateBack() },
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalContentInsets.current),
                    )
                } else {
                    // A brick without details (e.g. restored state after a layout change) has nothing to show.
                    LaunchedEffect(route) { navigator.navigateBack() }
                }
            }

            entry<NavRoute.Nodes> {
                Box(Modifier.fillMaxSize().windowInsetsPadding(LocalContentInsets.current)) {
                    NodesScreen(
                        onAddServer = { navigator.navigateToConnectionEditor(null) },
                        onEditServer = navigator::navigateToConnectionEditor,
                    )
                }
            }

            entry<NavRoute.ConnectionEditor> { route ->
                Box(Modifier.fillMaxSize().windowInsetsPadding(LocalContentInsets.current)) {
                    ConnectionEditorScreen(
                        profileId = route.profileId,
                        onSaved = { navigator.navigateBack() },
                    )
                }
            }

            entry<NavRoute.Settings> {
                Box(Modifier.fillMaxSize().windowInsetsPadding(LocalContentInsets.current)) {
                    SettingsScreen()
                }
            }
        },
    )
}

@Preview
@Composable
private fun NascopeAppScaffoldPreview() {
    AppTheme {
        NascopeAppScaffold(
            dashboardContent = { _, _ -> },
        )
    }
}
