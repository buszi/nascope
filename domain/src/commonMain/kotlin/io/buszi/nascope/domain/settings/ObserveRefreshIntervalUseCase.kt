package io.buszi.nascope.domain.settings

import io.buszi.nascope.domain.model.settings.RefreshInterval
import kotlinx.coroutines.flow.Flow

fun interface ObserveRefreshIntervalUseCase {
    operator fun invoke(): Flow<RefreshInterval>
}
