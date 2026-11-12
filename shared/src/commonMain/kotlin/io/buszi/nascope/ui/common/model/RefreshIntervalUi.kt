package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.settings.RefreshInterval

/** UI representation of the telemetry refresh interval. */
@Immutable
enum class RefreshIntervalUi(val label: String) {
    S1("1s"),
    S2("2s"),
    S5("5s"),
    S10("10s"),
    ;

    /** The domain interval this option stands for. */
    fun toDomain(): RefreshInterval = when (this) {
        S1 -> RefreshInterval.S1
        S2 -> RefreshInterval.S2
        S5 -> RefreshInterval.S5
        S10 -> RefreshInterval.S10
    }

    companion object {
        fun from(data: RefreshInterval): RefreshIntervalUi = when (data) {
            RefreshInterval.S1 -> S1
            RefreshInterval.S2 -> S2
            RefreshInterval.S5 -> S5
            RefreshInterval.S10 -> S10
        }
    }
}
