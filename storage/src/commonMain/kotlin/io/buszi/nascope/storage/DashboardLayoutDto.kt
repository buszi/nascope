package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import kotlinx.serialization.Serializable

/** The stored form of a [DashboardLayout]: `{"order":[...],"hidden":[...]}`. A missing list reads as empty. */
@Serializable
internal data class DashboardLayoutDto(
    val order: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
) {
    fun toDomain() = DashboardLayout(
        order = order.map(::DashboardBrickKey),
        hidden = hidden.mapTo(mutableSetOf(), ::DashboardBrickKey),
    )

    companion object {
        fun from(layout: DashboardLayout) = DashboardLayoutDto(
            order = layout.order.map { it.value },
            hidden = layout.hidden.map { it.value },
        )
    }
}
