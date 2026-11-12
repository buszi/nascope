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
}
