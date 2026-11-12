package io.buszi.nascope.domain.model.settings

/** How often the fast telemetry plugins are polled. Slower plugins use a multiple of it. */
enum class RefreshInterval(val seconds: Int) {
    S1(1),
    S2(2),
    S5(5),
    S10(10),
    ;

    companion object {
        val Default: RefreshInterval = S2
    }
}
