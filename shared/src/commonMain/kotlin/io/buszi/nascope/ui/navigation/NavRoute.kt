package io.buszi.nascope.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.nav_destination_dashboard
import io.buszi.nascope.resources.nav_destination_nodes
import io.buszi.nascope.resources.nav_destination_settings
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.StringResource

/**
 * Type-safe Navigation 3 destination keys for Nascope.
 */
@Immutable
@Serializable
sealed interface NavRoute : NavKey {

    /** Primary modular telemetry dashboard. */
    @Serializable
    data object Dashboard : NavRoute

    /** Secondary deep-dive inspector / drill-down screen for a specific [brickId]. */
    @Serializable
    data class Details(val brickId: DashboardBrickId) : NavRoute

    /** Multi-server node management destination. */
    @Serializable
    data object Nodes : NavRoute

    /** Add a server, or edit the stored one with [profileId], inside the main shell. Part of the Nodes destination. */
    @Serializable
    data class ConnectionEditor(val profileId: String?) : NavRoute

    /** Application preferences and telemetry polling configuration destination. */
    @Serializable
    data object Settings : NavRoute
}

/**
 * Top-level destinations accessible from the persistent desktop sidebar, tablet icon rail,
 * and mobile bottom navigation bar.
 */
@Immutable
enum class TopLevelDestination(
    val route: NavRoute,
    val labelRes: StringResource,
) {
    Dashboard(
        route = NavRoute.Dashboard,
        labelRes = Res.string.nav_destination_dashboard,
    ),
    Nodes(
        route = NavRoute.Nodes,
        labelRes = Res.string.nav_destination_nodes,
    ),
    Settings(
        route = NavRoute.Settings,
        labelRes = Res.string.nav_destination_settings,
    ),
}

/**
 * Resolves the [TopLevelDestination] associated with this [NavRoute].
 *
 * [NavRoute.Details] belongs to the [TopLevelDestination.Dashboard] hierarchy and
 * [NavRoute.ConnectionEditor] to the [TopLevelDestination.Nodes] hierarchy.
 */
fun NavRoute.toTopLevelDestination(): TopLevelDestination = when (this) {
    NavRoute.Dashboard, is NavRoute.Details -> TopLevelDestination.Dashboard
    NavRoute.Nodes, is NavRoute.ConnectionEditor -> TopLevelDestination.Nodes
    NavRoute.Settings -> TopLevelDestination.Settings
}

/**
 * Multiplatform [SavedStateConfiguration] registering all [NavRoute] keys for Navigation 3 state restoration.
 */
val NavRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.Dashboard::class)
            subclass(NavRoute.Details::class)
            subclass(NavRoute.Nodes::class)
            subclass(NavRoute.ConnectionEditor::class)
            subclass(NavRoute.Settings::class)
        }
    }
}
