package io.buszi.nascope.data.telemetry

import io.buszi.nascope.ui.dashboard.widgets.SystemOverviewData
import io.buszi.nascope.ui.dashboard.widgets.SystemOverviewPreviewParameterProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Shared singleton state holder managing telemetry polling, caching, and distribution.
 *
 * Acts as the central hub connecting background polling engines to modular widget ViewModels.
 */
@Single
class TelemetryStateHolder {

    private val _systemOverview = MutableStateFlow(SystemOverviewPreviewParameterProvider.UbuntuServer)
    val systemOverview: StateFlow<SystemOverviewData> = _systemOverview.asStateFlow()

    /**
     * Triggers a manual telemetry refresh.
     */
    fun refreshSystemOverview() {
        _systemOverview.value = SystemOverviewPreviewParameterProvider.UbuntuServer
    }
}
