package io.buszi.nascope.domain.foreground.impl

import io.buszi.nascope.domain.foreground.AppForegroundState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds the foreground flag. A `true` is applied immediately and cancels a pending `false`; a `false` is applied
 * only after [BackgroundGraceMillis] ms, so a rotation or a quick app switch never reaches the observers.
 */
@Single
internal class AppForegroundStateImpl(
    applicationScope: CoroutineScope,
) : AppForegroundState {

    private val requested = MutableStateFlow(false)
    private val published = MutableStateFlow(false)

    override val isForeground: Flow<Boolean> = published.asStateFlow()

    init {
        applicationScope.launch {
            requested.collectLatest { foreground ->
                if (!foreground) delay(BackgroundGraceMillis.milliseconds)
                published.value = foreground
            }
        }
    }

    override fun set(isForeground: Boolean) {
        requested.value = isForeground
    }

    companion object {
        const val BackgroundGraceMillis = 5_000L
    }
}
