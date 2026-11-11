package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnToggleMount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class StorageDetailsViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnSelectSort -> mutableViewState.update { it.copy(sortOrder = action.sortOrder) }
            is OnToggleMount -> toggleMount(action.mountPoint)
        }
    }

    private fun toggleMount(mountPoint: String) {
        mutableViewState.update { state ->
            state.copy(
                expandedMountPoint = if (state.expandedMountPoint == mountPoint) null else mountPoint,
            )
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.storagePools.onEach { storageData ->
            mutableViewState.update { current ->
                current.copy(
                    mounts = storageData.mounts,
                    readRateBytesPerSec = storageData.readRateBytesPerSec,
                    writeRateBytesPerSec = storageData.writeRateBytesPerSec,
                    readHistory = storageData.readHistory,
                    writeHistory = storageData.writeHistory,
                    expandedMountPoint = current.expandedMountPoint
                        ?.takeIf { expanded -> storageData.mounts.any { it.mountPoint == expanded } },
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    enum class SortOrder {
        Usage,
        Name,
        Size,
    }

    @Stable
    data class ViewState(
        val mounts: List<StorageMountData> = emptyList(),
        val readRateBytesPerSec: Long = 0L,
        val writeRateBytesPerSec: Long = 0L,
        val readHistory: List<Float> = emptyList(),
        val writeHistory: List<Float> = emptyList(),
        val sortOrder: SortOrder = SortOrder.Usage,
        val expandedMountPoint: String? = null,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val sortedMounts: List<StorageMountData>
            get() = when (sortOrder) {
                SortOrder.Usage -> mounts.sortedByDescending { it.usageRatio }
                SortOrder.Name -> mounts.sortedBy { it.mountPoint }
                SortOrder.Size -> mounts.sortedByDescending { it.totalBytes }
            }

        val totalCapacityBytes: Long
            get() = mounts.sumOf { it.totalBytes }

        val totalUsedBytes: Long
            get() = mounts.sumOf { it.usedBytes }

        val totalFreeBytes: Long
            get() = mounts.sumOf { it.freeBytes }

        val totalUsageRatio: Float
            get() = if (totalCapacityBytes > 0L) {
                (totalUsedBytes.toFloat() / totalCapacityBytes).coerceIn(0f, 1f)
            } else {
                0f
            }

        val overallStatus: StatusLevel
            get() = mounts.overallStatus
    }

    sealed interface ViewAction {
        data class OnSelectSort(val sortOrder: SortOrder) : ViewAction
        data class OnToggleMount(val mountPoint: String) : ViewAction
    }
}
