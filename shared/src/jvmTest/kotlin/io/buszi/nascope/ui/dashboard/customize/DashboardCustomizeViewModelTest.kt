package io.buszi.nascope.ui.dashboard.customize

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.dashboard.ObserveDashboardLayoutUseCase
import io.buszi.nascope.domain.dashboard.SetDashboardLayoutUseCase
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi.BrickRow
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnCancelReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnConfirmReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragCancel
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragMove
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragStart
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDrop
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnMoveDown
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnMoveUp
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnRequestReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnToggleBrick
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Alerts
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.Gpu
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId.SystemOverview
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsBrick
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardCustomizeViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val brickRegistry = mockk<BrickRegistry>()
    private val observeDashboardLayout = mockk<ObserveDashboardLayoutUseCase>()
    private val setDashboardLayout = mockk<SetDashboardLayoutUseCase>()
    private val observeAvailablePlugins = mockk<ObserveAvailablePluginsUseCase>()
    private val plugins = MutableStateFlow<Set<String>?>(null)

    /** The stored layout; `null` stands for "not read yet". A write lands here, like the real store. */
    private val stored = MutableStateFlow<DashboardLayout?>(DashboardLayout.Default)
    private val writes = mutableListOf<DashboardLayout>()

    /** Whether a write reaches [stored]; `false` holds the store back, as a slow disk would. */
    private var storeAnswers = true

    private val viewModel by lazy {
        every { brickRegistry.allBricks } returns listOf(SystemOverviewBrick, AlertsBrick, GpuBrick)
        every { observeDashboardLayout() } returns stored.filterNotNull()
        every { observeAvailablePlugins() } returns plugins
        coEvery { setDashboardLayout(any()) } answers {
            writes += firstArg<DashboardLayout>()
            if (storeAnswers) stored.value = firstArg()
        }
        DashboardCustomizeViewModel(
            brickRegistry = brickRegistry,
            observeDashboardLayout = observeDashboardLayout,
            setDashboardLayout = setDashboardLayout,
            observeAvailablePlugins = observeAvailablePlugins,
        )
    }

    private fun rows(vararg rows: BrickRow) = rows.toList()

    @Test
    fun dashboardCustomizeViewModelShowsNothingBeforeTheLayoutIsReadThenTheStoredLayout() = runTest {
        stored.value = null
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            stored.value = DashboardLayout(order = listOf(Gpu.key), hidden = setOf(Alerts.key))
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    ViewState(
                        customize = DashboardCustomizeUi(
                            rows = rows(BrickRow(SystemOverview), BrickRow(Alerts, isEnabled = false), BrickRow(Gpu)),
                            isCustomized = true,
                        ),
                    ),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun dashboardCustomizeViewModelWritesTheCompleteLayoutOnASwitch() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnToggleBrick(Gpu, enabled = false))
        runCurrent()

        assertEquals(
            listOf(
                DashboardLayout(order = listOf(SystemOverview.key, Alerts.key, Gpu.key), hidden = setOf(Gpu.key)),
            ),
            writes,
        )
        assertEquals(
            rows(BrickRow(SystemOverview), BrickRow(Alerts), BrickRow(Gpu, isEnabled = false)),
            viewModel.viewState.value.customize?.rows,
        )

        viewModel.onViewAction(OnToggleBrick(Gpu, enabled = true))
        runCurrent()

        assertEquals(
            DashboardLayout(order = listOf(SystemOverview.key, Alerts.key, Gpu.key), hidden = emptySet()),
            writes.last(),
        )
    }

    @Test
    fun dashboardCustomizeViewModelKeepsEverySwitchFlippedBeforeTheStoreAnswers() = runTest {
        viewModel.viewState.value
        runCurrent()
        storeAnswers = false

        viewModel.onViewAction(OnToggleBrick(Alerts, enabled = false))
        viewModel.onViewAction(OnToggleBrick(Gpu, enabled = false))
        runCurrent()

        assertEquals(setOf(Alerts.key, Gpu.key), writes.last().hidden)
        assertEquals(listOf(true, false, false), viewModel.viewState.value.customize?.rows?.map { it.isEnabled })
    }

    @Test
    fun dashboardCustomizeViewModelWritesNothingForASwitchThatChangesNothing() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnToggleBrick(Gpu, enabled = true))
        runCurrent()

        assertEquals(emptyList(), writes)
    }

    @Test
    fun dashboardCustomizeViewModelWritesTheCompleteNewOrderOnAMove() = runTest {
        stored.value = DashboardLayout(order = emptyList(), hidden = setOf(Gpu.key))
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnMoveUp(Gpu))
        runCurrent()
        viewModel.onViewAction(OnMoveUp(Gpu))
        runCurrent()
        viewModel.onViewAction(OnMoveDown(SystemOverview))
        runCurrent()

        assertEquals(
            listOf(
                DashboardLayout(order = listOf(SystemOverview.key, Gpu.key, Alerts.key), hidden = setOf(Gpu.key)),
                DashboardLayout(order = listOf(Gpu.key, SystemOverview.key, Alerts.key), hidden = setOf(Gpu.key)),
                DashboardLayout(order = listOf(Gpu.key, Alerts.key, SystemOverview.key), hidden = setOf(Gpu.key)),
            ),
            writes,
        )
        assertEquals(
            rows(BrickRow(Gpu, isEnabled = false), BrickRow(Alerts), BrickRow(SystemOverview)),
            viewModel.viewState.value.customize?.rows,
        )
    }

    @Test
    fun dashboardCustomizeViewModelKeepsEveryMoveMadeBeforeTheStoreAnswers() = runTest {
        viewModel.viewState.value
        runCurrent()
        storeAnswers = false

        viewModel.onViewAction(OnMoveUp(Gpu))
        viewModel.onViewAction(OnMoveUp(Gpu))
        runCurrent()

        assertEquals(listOf(Gpu.key, SystemOverview.key, Alerts.key), writes.last().order)
        assertEquals(listOf(Gpu, SystemOverview, Alerts), viewModel.viewState.value.customize?.rows?.map { it.id })
    }

    @Test
    fun dashboardCustomizeViewModelWritesNothingForAMovePastAnEnd() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnMoveUp(SystemOverview))
        viewModel.onViewAction(OnMoveDown(Gpu))
        runCurrent()

        assertEquals(emptyList(), writes)
    }

    @Test
    fun dashboardCustomizeViewModelMovesRowsDuringADragAndWritesOnceOnDrop() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnDragStart)
        viewModel.onViewAction(OnDragMove(Gpu, offset = -1))
        runCurrent()
        assertEquals(listOf(SystemOverview, Gpu, Alerts), viewModel.viewState.value.customize?.rows?.map { it.id })
        viewModel.onViewAction(OnDragMove(Gpu, offset = -1))
        runCurrent()
        assertEquals(listOf(Gpu, SystemOverview, Alerts), viewModel.viewState.value.customize?.rows?.map { it.id })
        assertEquals(emptyList(), writes)

        viewModel.onViewAction(OnDrop)
        runCurrent()

        assertEquals(listOf(DashboardLayout(order = listOf(Gpu.key, SystemOverview.key, Alerts.key), hidden = emptySet())), writes)
        assertEquals(listOf(Gpu, SystemOverview, Alerts), viewModel.viewState.value.customize?.rows?.map { it.id })
    }

    @Test
    fun dashboardCustomizeViewModelWritesNothingForADropBackInPlace() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnDragStart)
        viewModel.onViewAction(OnDragMove(Alerts, offset = 1))
        viewModel.onViewAction(OnDragMove(Alerts, offset = -1))
        viewModel.onViewAction(OnDrop)
        runCurrent()

        assertEquals(emptyList(), writes)
    }

    @Test
    fun dashboardCustomizeViewModelRestoresTheStoredOrderOnACancelledDrag() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnDragStart)
        viewModel.onViewAction(OnDragMove(SystemOverview, offset = 1))
        viewModel.onViewAction(OnDragCancel)
        runCurrent()
        viewModel.onViewAction(OnDrop)
        runCurrent()

        assertEquals(emptyList(), writes)
        assertEquals(listOf(SystemOverview, Alerts, Gpu), viewModel.viewState.value.customize?.rows?.map { it.id })
    }

    @Test
    fun dashboardCustomizeViewModelKeepsTheDraggedListWhenTheLayoutChangesElsewhereAndKeepsThatChangeOnDrop() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnDragStart)
        viewModel.onViewAction(OnDragMove(Gpu, offset = -1))
        runCurrent()
        // Another window turns Alerts off meanwhile.
        stored.value = DashboardLayout(order = emptyList(), hidden = setOf(Alerts.key))
        runCurrent()
        assertEquals(
            rows(BrickRow(SystemOverview), BrickRow(Gpu), BrickRow(Alerts)),
            viewModel.viewState.value.customize?.rows,
        )

        viewModel.onViewAction(OnDrop)
        runCurrent()

        assertEquals(
            listOf(DashboardLayout(order = listOf(SystemOverview.key, Gpu.key, Alerts.key), hidden = setOf(Alerts.key))),
            writes,
        )
        assertEquals(
            rows(BrickRow(SystemOverview), BrickRow(Gpu), BrickRow(Alerts, isEnabled = false)),
            viewModel.viewState.value.customize?.rows,
        )
    }

    @Test
    fun dashboardCustomizeViewModelIgnoresDragMovesOutsideADragAndEditsDuringOne() = runTest {
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnDragMove(Gpu, offset = -1))
        runCurrent()
        assertEquals(listOf(SystemOverview, Alerts, Gpu), viewModel.viewState.value.customize?.rows?.map { it.id })

        viewModel.onViewAction(OnDragStart)
        viewModel.onViewAction(OnToggleBrick(Alerts, enabled = false))
        viewModel.onViewAction(OnMoveUp(Gpu))
        runCurrent()

        assertEquals(emptyList(), writes)
        assertTrue(viewModel.viewState.value.customize!!.rows.all { it.isEnabled })
    }

    @Test
    fun dashboardCustomizeViewModelResetBringsBackTheRegistryOrder() = runTest {
        viewModel.viewState.value
        runCurrent()
        viewModel.onViewAction(OnMoveDown(SystemOverview))
        viewModel.onViewAction(OnToggleBrick(Alerts, enabled = false))
        runCurrent()
        assertEquals(listOf(Alerts, SystemOverview, Gpu), viewModel.viewState.value.customize?.rows?.map { it.id })

        viewModel.onViewAction(OnRequestReset)
        viewModel.onViewAction(OnConfirmReset)
        runCurrent()

        assertEquals(DashboardLayout.Default, writes.last())
        assertEquals(
            DashboardCustomizeUi(rows = rows(BrickRow(SystemOverview), BrickRow(Alerts), BrickRow(Gpu))),
            viewModel.viewState.value.customize,
        )
    }

    @Test
    fun dashboardCustomizeViewModelFollowsOutsideLayoutAndPluginChanges() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            stored.value = DashboardLayout(order = listOf(Gpu.key, Alerts.key, SystemOverview.key), hidden = emptySet())
            runCurrent()
            plugins.value = setOf("alert")
            runCurrent()

            assertEquals(
                listOf(
                    null,
                    rows(BrickRow(SystemOverview), BrickRow(Alerts), BrickRow(Gpu)),
                    rows(BrickRow(Gpu), BrickRow(Alerts), BrickRow(SystemOverview)),
                    rows(BrickRow(Gpu, isOffered = false), BrickRow(Alerts), BrickRow(SystemOverview)),
                ),
                viewStateTurbine.cancelAndConsumeItems().map { it.customize?.rows },
            )
        }
    }

    @Test
    fun dashboardCustomizeViewModelCannotResetTheDefaultLayout() = runTest {
        viewModel.viewState.value
        runCurrent()

        assertFalse(viewModel.viewState.value.customize!!.isCustomized)
        viewModel.onViewAction(OnRequestReset)
        viewModel.onViewAction(OnConfirmReset)
        runCurrent()

        assertFalse(viewModel.viewState.value.isConfirmingReset)
        assertEquals(emptyList(), writes)
    }

    @Test
    fun dashboardCustomizeViewModelResetsOnlyAfterTheConfirmation() = runTest {
        stored.value = DashboardLayout(order = listOf(Gpu.key), hidden = setOf(Alerts.key))
        viewModel.viewState.value
        runCurrent()

        viewModel.onViewAction(OnConfirmReset)
        runCurrent()
        assertEquals(emptyList(), writes)

        viewModel.onViewAction(OnRequestReset)
        runCurrent()
        assertTrue(viewModel.viewState.value.isConfirmingReset)
        assertEquals(emptyList(), writes)

        viewModel.onViewAction(OnCancelReset)
        runCurrent()
        assertFalse(viewModel.viewState.value.isConfirmingReset)
        assertEquals(emptyList(), writes)

        viewModel.onViewAction(OnRequestReset)
        viewModel.onViewAction(OnConfirmReset)
        runCurrent()

        assertEquals(listOf(DashboardLayout.Default), writes)
        assertEquals(
            ViewState(
                customize = DashboardCustomizeUi(rows = rows(BrickRow(SystemOverview), BrickRow(Alerts), BrickRow(Gpu))),
            ),
            viewModel.viewState.value,
        )
        coVerify(exactly = 1) { setDashboardLayout(any()) }
    }

    @Test
    fun dashboardCustomizeViewModelDropsAPendingResetWhenTheLayoutIsResetElsewhere() = runTest {
        stored.value = DashboardLayout(order = emptyList(), hidden = setOf(Alerts.key))
        viewModel.viewState.value
        runCurrent()
        viewModel.onViewAction(OnRequestReset)
        runCurrent()

        stored.value = DashboardLayout.Default
        runCurrent()

        assertFalse(viewModel.viewState.value.isConfirmingReset)
        assertNull(writes.firstOrNull())
    }
}
