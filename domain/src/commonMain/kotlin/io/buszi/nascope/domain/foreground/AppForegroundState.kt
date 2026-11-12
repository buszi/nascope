package io.buszi.nascope.domain.foreground

import kotlinx.coroutines.flow.Flow

internal interface AppForegroundState {
    /** `true` while the app is visible. Starts as `false`; going to background is reported after a 5 s grace period. */
    val isForeground: Flow<Boolean>

    fun set(isForeground: Boolean)
}
