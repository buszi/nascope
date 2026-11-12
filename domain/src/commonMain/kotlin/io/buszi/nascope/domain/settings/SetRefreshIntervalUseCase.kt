package io.buszi.nascope.domain.settings

import io.buszi.nascope.domain.model.settings.RefreshInterval

fun interface SetRefreshIntervalUseCase {
    suspend operator fun invoke(interval: RefreshInterval)
}
