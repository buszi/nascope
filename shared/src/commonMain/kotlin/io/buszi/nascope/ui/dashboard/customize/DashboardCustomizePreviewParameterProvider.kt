package io.buszi.nascope.ui.dashboard.customize

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi.BrickRow
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/** Preview parameters of the dashboard editor: default, some bricks off, an unavailable brick, everything off. */
class DashboardCustomizePreviewParameterProvider : PreviewParameterProvider<ViewState> {

    /** The registry order, every brick on. */
    private val defaultRows = BrickRegistry().allBricks.map { BrickRow(id = it.id) }

    override val values: Sequence<ViewState> = sequenceOf(
        ViewState(customize = DashboardCustomizeUi(rows = defaultRows)),
        ViewState(
            customize = DashboardCustomizeUi(
                rows = defaultRows.map {
                    it.copy(isEnabled = it.id != DashboardBrickId.Alerts && it.id != DashboardBrickId.Containers)
                },
                isCustomized = true,
            ),
        ),
        ViewState(
            customize = DashboardCustomizeUi(
                rows = defaultRows.map { it.copy(isOffered = it.id != DashboardBrickId.Gpu) },
                isCustomized = true,
            ),
            isConfirmingReset = true,
        ),
        ViewState(
            customize = DashboardCustomizeUi(rows = defaultRows.map { it.copy(isEnabled = false) }, isCustomized = true),
        ),
    )
}
