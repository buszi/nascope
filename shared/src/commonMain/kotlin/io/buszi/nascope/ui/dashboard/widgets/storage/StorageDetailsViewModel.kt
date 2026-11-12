package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.DiskIoUi
import io.buszi.nascope.ui.common.model.DiskUi
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.common.model.StorageUi
import io.buszi.nascope.ui.common.model.distinctDevices
import io.buszi.nascope.ui.common.model.overallStatus
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnDisksPage
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
    private val observeStorage: ObserveStorageUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
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
            is OnDisksPage -> mutableViewState.update { it.copy(disksPage = (it.disksPage + action.delta).coerceIn(0, it.disksPageCount - 1)) }
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
        observeStorage().onEach { state ->
            mutableViewState.update { current ->
                val storage = when (state) {
                    is TelemetryState.Ready -> StorageUi.from(state.data)
                    TelemetryState.Loading -> StorageUi()
                    is TelemetryState.Failed -> StorageUi(current.mounts, current.diskIo, current.disks)
                }
                current.copy(
                    mounts = storage.mounts,
                    diskIo = storage.diskIo,
                    disks = storage.disks,
                    disksPage = current.disksPage.coerceIn(0, maxOf(0, (storage.disks.size - 1) / DisksPageSize)),
                    expandedMountPoint = current.expandedMountPoint
                        ?.takeIf { expanded -> storage.mounts.any { it.mountPoint == expanded } },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
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
        val mounts: List<StorageMountUi> = emptyList(),
        val diskIo: DiskIoUi? = null,
        /** Busiest first. */
        val disks: List<DiskUi> = emptyList(),
        val disksPage: Int = 0,
        val sortOrder: SortOrder = SortOrder.Usage,
        val expandedMountPoint: String? = null,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val disksPageCount: Int
            get() = maxOf(1, (disks.size + DisksPageSize - 1) / DisksPageSize)

        val pagedDisks: List<DiskUi>
            get() = disks.drop(disksPage * DisksPageSize).take(DisksPageSize)

        val sortedMounts: List<StorageMountUi>
            get() = when (sortOrder) {
                SortOrder.Usage -> mounts.sortedByDescending { it.usageRatio }
                SortOrder.Name -> mounts.sortedBy { it.displayName }
                SortOrder.Size -> mounts.sortedByDescending { it.totalBytes }
            }

        val totalCapacityBytes: Long
            get() = mounts.distinctDevices.sumOf { it.totalBytes }

        val totalUsedBytes: Long
            get() = mounts.distinctDevices.sumOf { it.usedBytes }

        val totalFreeBytes: Long
            get() = mounts.distinctDevices.sumOf { it.freeBytes }

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

        /** Moves the disks section by [delta] pages (-1 or 1). */
        data class OnDisksPage(val delta: Int) : ViewAction
    }
}

/** Disks per page of the disks section: a NAS can report dozens of devices, partitions included. */
const val DisksPageSize = 5
