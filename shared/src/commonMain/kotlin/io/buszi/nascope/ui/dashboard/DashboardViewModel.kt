package io.buszi.nascope.ui.dashboard

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(
    brickRegistry: BrickRegistry,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(
        ViewState(bricks = brickRegistry.getActiveBricks()),
    )
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    @Stable
    data class ViewState(
        val bricks: List<DashboardBrick> = emptyList(),
    )
}
