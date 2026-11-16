package io.buszi.nascope.domain.dashboard

import io.buszi.nascope.domain.model.dashboard.DashboardLayout

/** Stores a complete dashboard layout; [DashboardLayout.Default] resets it. */
fun interface SetDashboardLayoutUseCase {
    suspend operator fun invoke(layout: DashboardLayout)
}
