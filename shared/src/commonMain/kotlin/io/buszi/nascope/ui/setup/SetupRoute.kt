package io.buszi.nascope.ui.setup

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Navigation 3 destination keys of the setup flow shown before any server is configured. */
@Immutable
@Serializable
sealed interface SetupRoute : NavKey {

    /** First screen: what Nascope does, and the choice between connecting a server and demo data. */
    @Serializable
    data object Welcome : SetupRoute

    /** Connection form for the first server. */
    @Serializable
    data object Connection : SetupRoute
}

/** Registers all [SetupRoute] keys for Navigation 3 state restoration. */
val SetupRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SetupRoute.Welcome::class)
            subclass(SetupRoute.Connection::class)
        }
    }
}
