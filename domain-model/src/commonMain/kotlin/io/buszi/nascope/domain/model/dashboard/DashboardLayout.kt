package io.buszi.nascope.domain.model.dashboard

/**
 * The user's dashboard layout. [order] lists brick keys first to last; [hidden] are the bricks turned off.
 * Both may name keys this app version does not know and may lack keys it does; the UI resolves that.
 */
data class DashboardLayout(
    val order: List<DashboardBrickKey>,
    val hidden: Set<DashboardBrickKey>,
) {
    companion object {
        /** Nothing customized: the built-in order, every brick on. */
        val Default = DashboardLayout(order = emptyList(), hidden = emptySet())
    }
}
