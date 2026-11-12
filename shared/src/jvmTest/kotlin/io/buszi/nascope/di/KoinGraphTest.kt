package io.buszi.nascope.di

import io.buszi.nascope.domain.connection.DeleteServerProfileUseCase
import io.buszi.nascope.domain.connection.GetServerProfileUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveServerProfilesUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.SaveServerProfileUseCase
import io.buszi.nascope.domain.connection.SetActiveServerUseCase
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.domain.connection.TestConnectionUseCase
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase
import io.buszi.nascope.domain.foreground.SetAppForegroundUseCase
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.domain.settings.SetRefreshIntervalUseCase
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveProgramsUseCase
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.koinConfiguration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/** The root graph must resolve every use case across `:domain`, `:network` and `:storage`. */
class KoinGraphTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun resolvesAllConnectionUseCases() {
        val koin = startKoin(koinConfiguration<NascopeApp>()).koin

        assertNotNull(koin.get<ObserveSessionUseCase>())
        assertNotNull(koin.get<ObserveConnectionStatusUseCase>())
        assertNotNull(koin.get<ObserveServerProfilesUseCase>())
        assertNotNull(koin.get<GetServerProfileUseCase>())
        assertNotNull(koin.get<ValidateServerInputUseCase>())
        assertNotNull(koin.get<TestConnectionUseCase>())
        assertNotNull(koin.get<SaveServerProfileUseCase>())
        assertNotNull(koin.get<DeleteServerProfileUseCase>())
        assertNotNull(koin.get<SetActiveServerUseCase>())
        assertNotNull(koin.get<SetDemoModeUseCase>())
    }

    @Test
    fun resolvesTelemetryForegroundAndSettingsUseCases() {
        val koin = startKoin(koinConfiguration<NascopeApp>()).koin

        assertNotNull(koin.get<SetAppForegroundUseCase>())
        assertNotNull(koin.get<ObserveRefreshIntervalUseCase>())
        assertNotNull(koin.get<SetRefreshIntervalUseCase>())
        assertNotNull(koin.get<ObserveSystemOverviewUseCase>())
        assertNotNull(koin.get<ObserveMemoryUseCase>())
        assertNotNull(koin.get<ObserveContainersUseCase>())
        assertNotNull(koin.get<ObserveProcessesUseCase>())
        assertNotNull(koin.get<RefreshTelemetryUseCase>())
        assertNotNull(koin.get<ObserveCpuUseCase>())
        assertNotNull(koin.get<ObserveStorageUseCase>())
        assertNotNull(koin.get<ObserveNetworkUseCase>())
        assertNotNull(koin.get<ObserveSensorsUseCase>())
        assertNotNull(koin.get<ObserveGpuUseCase>())
        assertNotNull(koin.get<ObserveAvailablePluginsUseCase>())
        assertNotNull(koin.get<ObserveAlertsUseCase>())
        assertNotNull(koin.get<ObserveProgramsUseCase>())
    }
}
