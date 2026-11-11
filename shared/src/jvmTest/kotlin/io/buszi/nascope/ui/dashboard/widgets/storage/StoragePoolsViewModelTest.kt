package io.buszi.nascope.ui.dashboard.widgets.storage

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.data.telemetry.StoragePoolsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class StoragePoolsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        StoragePoolsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun storagePoolsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            val storagePoolsFlow = MutableStateFlow(storageData)
            every { telemetryStateHolder.storagePools } returns storagePoolsFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.storagePools
            }
        }
    }

    @Test
    fun storagePoolsViewModelHandlesRetryAction() = runTest {
        turbineScope {
            val storagePoolsFlow = MutableStateFlow(storageData)
            every { telemetryStateHolder.storagePools } returns storagePoolsFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnRetry)

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.storagePools
                telemetryStateHolder.refresh()
            }
        }
    }

    @Test
    fun storagePoolsViewModelHandlesPagination() = runTest {
        turbineScope {
            val storagePoolsFlow = MutableStateFlow(storageData)
            every { telemetryStateHolder.storagePools } returns storagePoolsFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(0, viewModel.viewState.value.currentPage)
            assertEquals(2, viewModel.viewState.value.totalPages)
            assertEquals(3, viewModel.viewState.value.displayedMounts.size)

            viewModel.onViewAction(OnNextPage)
            assertEquals(1, viewModel.viewState.value.currentPage)
            assertEquals(1, viewModel.viewState.value.displayedMounts.size)
            assertEquals("/mnt/backup", viewModel.viewState.value.displayedMounts.first().mountPoint)

            // Advancing past max page does not exceed bounds
            viewModel.onViewAction(OnNextPage)
            assertEquals(1, viewModel.viewState.value.currentPage)

            viewModel.onViewAction(OnPrevPage)
            assertEquals(0, viewModel.viewState.value.currentPage)

            // Decrementing past 0 stays at 0
            viewModel.onViewAction(OnPrevPage)
            assertEquals(0, viewModel.viewState.value.currentPage)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storagePoolsViewModelComputesAggregatesAndStatus() = runTest {
        val mounts = listOf(
            StorageMountUi("/", "/dev/nvme0n1", "ext4", 1_000_000_000L, 500_000_000L, 500_000_000L, true),
            StorageMountUi("/data", "/dev/sda1", "btrfs", 2_000_000_000L, 1_800_000_000L, 200_000_000L, true), // 90%
        )
        val state = ViewState(mounts = mounts)
        assertEquals(3_000_000_000L, state.totalCapacityBytes)
        assertEquals(2_300_000_000L, state.totalUsedBytes)
        assertEquals(StatusLevel.Critical, state.overallStatus)
    }

    companion object {
        private val storageData = StoragePoolsData(
            mounts = listOf(
                StorageMountData(
                    mountPoint = "/",
                    deviceName = "/dev/nvme0n1p2",
                    fsType = "ext4",
                    totalBytes = 512_000_000_000L,
                    usedBytes = 153_600_000_000L,
                    freeBytes = 358_400_000_000L,
                    isHealthy = true,
                ),
                StorageMountData(
                    mountPoint = "/volume1",
                    deviceName = "/dev/mapper/vg0-data",
                    fsType = "btrfs",
                    totalBytes = 3_800_000_000_000L,
                    usedBytes = 2_470_000_000_000L,
                    freeBytes = 1_330_000_000_000L,
                    isHealthy = true,
                ),
                StorageMountData(
                    mountPoint = "/zpool/media",
                    deviceName = "tank/media",
                    fsType = "zfs",
                    totalBytes = 8_000_000_000_000L,
                    usedBytes = 6_880_000_000_000L,
                    freeBytes = 1_120_000_000_000L,
                    isHealthy = false,
                ),
                StorageMountData(
                    mountPoint = "/mnt/backup",
                    deviceName = "/dev/sdc1",
                    fsType = "ext4",
                    totalBytes = 2_000_000_000_000L,
                    usedBytes = 420_000_000_000L,
                    freeBytes = 1_580_000_000_000L,
                    isHealthy = true,
                ),
            ),
            readRateBytesPerSec = 42_500_000L,
            writeRateBytesPerSec = 128_300_000L,
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            mounts = storageData.mounts.map(StorageMountUi::from),
            readRateBytesPerSec = 42_500_000L,
            writeRateBytesPerSec = 128_300_000L,
            currentPage = 0,
            pageSize = 3,
            widgetState = WidgetState.Ready,
        )
    }
}
