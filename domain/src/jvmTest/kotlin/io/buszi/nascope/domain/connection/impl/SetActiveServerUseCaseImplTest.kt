package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.storage.ServerProfileStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class SetActiveServerUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val profileStore = mockk<ServerProfileStore>()
    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        SetActiveServerUseCaseImpl(
            profileStore = profileStore,
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun setActiveServerUseCaseSelectsProfileAndTurnsDemoModeOff() = runTest {
        coEvery { profileStore.setActiveProfileId(profileId) } just runs
        coEvery { preferencesStore.setDemoMode(false) } just runs

        useCase(profileId)

        coVerifySequence {
            profileStore.setActiveProfileId(profileId)
            preferencesStore.setDemoMode(false)
        }
    }

    companion object {
        private val profileId = ServerProfileId("alpha")
    }
}
