package io.buszi.nascope.ui.connection

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.GetServerProfileUseCase
import io.buszi.nascope.domain.connection.SaveServerProfileUseCase
import io.buszi.nascope.domain.connection.TestConnectionUseCase
import io.buszi.nascope.domain.connection.TestConnectionUseCase.Event
import io.buszi.nascope.domain.connection.TestConnectionUseCase.Step
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.AliasError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.HostError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.PortError
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.ConnectionCheckStatusUi
import io.buszi.nascope.ui.common.model.ConnectionCheckStepUi
import io.buszi.nascope.ui.common.model.ConnectionCheckUi
import io.buszi.nascope.ui.common.model.ConnectionFailureUi
import io.buszi.nascope.ui.common.model.FormErrorUi
import io.buszi.nascope.ui.common.model.ServerProtocolUi
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.Phase
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnAliasChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnAuthToggle
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnHostChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnPasswordChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnPortChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnProtocolSelect
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnSubmit
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnUsernameChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionEditorViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val validateServerInput = mockk<ValidateServerInputUseCase>()
    private val testConnection = mockk<TestConnectionUseCase>()
    private val saveServerProfile = mockk<SaveServerProfileUseCase>()
    private val getServerProfile = mockk<GetServerProfileUseCase>()

    private val viewModel by lazy { createViewModel(profileId = null) }

    private fun createViewModel(profileId: String?) = ConnectionEditorViewModel(
        profileId = profileId,
        validateServerInput = validateServerInput,
        testConnection = testConnection,
        saveServerProfile = saveServerProfile,
        getServerProfile = getServerProfile,
    )

    @Test
    fun connectionEditorViewModelStartsWithDefaultsAndPendingChecks() = runTest {
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(ViewState()),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            assertEquals("61208", ViewState().port)
            assertEquals("glances", ViewState().username)
        }
    }

    @Test
    fun connectionEditorViewModelShowsErrorsOnlyForEditedFieldsUntilSubmit() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns invalidInput

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnHostChange("http://nas.local"))
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    ViewState(host = "http://nas.local", hostError = FormErrorUi.HostContainsScheme),
                    ViewState(
                        host = "http://nas.local",
                        aliasError = FormErrorUi.AliasBlank,
                        hostError = FormErrorUi.HostContainsScheme,
                        portError = FormErrorUi.PortBlank,
                    ),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            coVerify(exactly = 0) { testConnection(any(), any(), any()) }
        }
    }

    @Test
    fun connectionEditorViewModelDoesNotTestWhenUsernameIsBlankWithPasswordProtection() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnAuthToggle(true))
            viewModel.onViewAction(OnUsernameChange(""))
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            assertEquals(
                ViewState(authEnabled = true, username = "", usernameError = FormErrorUi.UsernameBlank),
                viewStateTurbine.cancelAndConsumeItems().last(),
            )
            coVerify(exactly = 0) { testConnection(any(), any(), any()) }
        }
    }

    @Test
    fun connectionEditorViewModelMapsTestEventsToTheCheckList() = runTest {
        turbineScope {
            val events = MutableSharedFlow<Event>()
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns events

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()
            events.emit(Event.StepStarted(Step.ReachHost))
            runCurrent()
            events.emit(Event.StepPassed(Step.ReachHost))
            runCurrent()
            events.emit(Event.StepStarted(Step.DetectGlances))
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(),
                    ViewState(phase = Phase.Testing),
                    ViewState(phase = Phase.Testing, checks = checks(Running, Pending, Pending)),
                    ViewState(phase = Phase.Testing, checks = checks(Passed, Pending, Pending)),
                    ViewState(phase = Phase.Testing, checks = checks(Passed, Running, Pending)),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun connectionEditorViewModelSavesTheProfileOnlyWhenTheTestSucceeds() = runTest {
        turbineScope {
            val savedProfile = slot<ServerProfile>()
            val savedPassword = slot<String?>()
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns successfulTest(GlancesApiVersion.V3)
            coEvery { saveServerProfile(capture(savedProfile), captureNullable(savedPassword)) } returns
                SaveServerProfileUseCase.Result(secretPersisted = true)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnAliasChange(" NAS rack 2 "))
            viewModel.onViewAction(OnProtocolSelect(ServerProtocolUi.Https))
            viewModel.onViewAction(OnHostChange(" nas.local "))
            viewModel.onViewAction(OnPortChange("8443"))
            viewModel.onViewAction(OnAuthToggle(true))
            viewModel.onViewAction(OnUsernameChange("admin"))
            viewModel.onViewAction(OnPasswordChange("secret"))
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(Phase.Saved, finalState.phase)
            assertEquals(checks(Passed, Passed, Passed), finalState.checks)
            assertEquals(false, finalState.showSecretNotPersistedNotice)
            assertEquals("NAS rack 2", savedProfile.captured.alias)
            assertEquals(ServerAddress(ServerProtocol.Https, "nas.local", 8443), savedProfile.captured.address)
            assertEquals(GlancesApiVersion.V3, savedProfile.captured.apiVersion)
            assertEquals(ServerAuth.Basic("admin"), savedProfile.captured.auth)
            assertTrue(savedProfile.captured.id.value.isNotBlank())
            assertEquals("secret", savedPassword.captured)
            coVerify {
                testConnection(
                    ServerAddress(ServerProtocol.Https, "nas.local", 8443),
                    ServerCredentials("admin", "secret"),
                    null,
                )
            }
        }
    }

    @Test
    fun connectionEditorViewModelSavesWithoutAuthWhenPasswordProtectionIsOff() = runTest {
        turbineScope {
            val savedProfile = slot<ServerProfile>()
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns successfulTest(GlancesApiVersion.V4)
            coEvery { saveServerProfile(capture(savedProfile), null) } returns
                SaveServerProfileUseCase.Result(secretPersisted = true)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            assertEquals(ServerAuth.None, savedProfile.captured.auth)
            coVerify { testConnection(any(), null, null) }
        }
    }

    @Test
    fun connectionEditorViewModelWarnsWhenThePasswordIsNotStoredPersistently() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns successfulTest(GlancesApiVersion.V4)
            coEvery { saveServerProfile(any(), any()) } returns SaveServerProfileUseCase.Result(secretPersisted = false)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnAuthToggle(true))
            viewModel.onViewAction(OnPasswordChange("secret"))
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(Phase.Saved, finalState.phase)
            assertTrue(finalState.showSecretNotPersistedNotice)
        }
    }

    @Test
    fun connectionEditorViewModelDoesNotSaveAndKeepsFormEditableWhenTheTestFails() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns failedTest(Step.ReachHost, ConnectionFailure.UnknownHost)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(Phase.Idle, finalState.phase)
            assertTrue(finalState.isEditable)
            assertTrue(finalState.canSubmit)
            assertEquals(
                listOf(
                    ConnectionCheckUi(ConnectionCheckStepUi.ReachHost, Failed, ConnectionFailureUi.UnknownHost),
                    ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, Pending),
                    ConnectionCheckUi(ConnectionCheckStepUi.Authenticate, Pending),
                ),
                finalState.checks,
            )
            coVerify(exactly = 0) { saveServerProfile(any(), any()) }
        }
    }

    @Test
    fun connectionEditorViewModelRetriesAfterAFailure() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returnsMany listOf(
                failedTest(Step.ReachHost, ConnectionFailure.Timeout),
                successfulTest(GlancesApiVersion.V4),
            )
            coEvery { saveServerProfile(any(), any()) } returns SaveServerProfileUseCase.Result(secretPersisted = true)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(Phase.Saved, finalState.phase)
            assertEquals(checks(Passed, Passed, Passed), finalState.checks)
            verify(exactly = 2) { testConnection(any(), any(), any()) }
            coVerify(exactly = 1) { saveServerProfile(any(), any()) }
        }
    }

    @Test
    fun connectionEditorViewModelTurnsPasswordProtectionOnAndRequestsUsernameFocusWhenUnauthorized() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns failedTest(Step.Authenticate, ConnectionFailure.Unauthorized)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertTrue(finalState.authEnabled)
            assertEquals(1, finalState.usernameFocusRequest)
            assertEquals(ConnectionFailureUi.Unauthorized, finalState.checks.last().failure)
        }
    }

    @Test
    fun connectionEditorViewModelDoesNotRequestFocusWhenUnauthorizedWithPasswordProtectionAlreadyOn() = runTest {
        turbineScope {
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns failedTest(Step.Authenticate, ConnectionFailure.Unauthorized)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnAuthToggle(true))
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            assertEquals(0, viewStateTurbine.cancelAndConsumeItems().last().usernameFocusRequest)
        }
    }

    @Test
    fun connectionEditorViewModelLoadsAnExistingProfileWithAnEmptyPassword() = runTest {
        turbineScope {
            coEvery { getServerProfile(existingProfile.id) } returns existingProfile

            val viewStateTurbine = createViewModel(profileId = "alpha").viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(isLoading = true, isEditMode = true),
                    ViewState(
                        isEditMode = true,
                        alias = "Alpha",
                        protocol = ServerProtocolUi.Https,
                        host = "alpha.local",
                        port = "8443",
                        authEnabled = true,
                        username = "admin",
                        password = "",
                    ),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun connectionEditorViewModelKeepsTheStoredSecretWhenEditingWithAnEmptyPassword() = runTest {
        turbineScope {
            coEvery { getServerProfile(existingProfile.id) } returns existingProfile
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns successfulTest(GlancesApiVersion.V4)
            coEvery { saveServerProfile(any(), any()) } returns SaveServerProfileUseCase.Result(secretPersisted = true)

            val editViewModel = createViewModel(profileId = "alpha")
            val viewStateTurbine = editViewModel.viewState.testIn(this)
            runCurrent()
            editViewModel.onViewAction(OnSubmit)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            coVerify {
                testConnection(
                    ServerAddress(ServerProtocol.Https, "alpha.local", 8443),
                    ServerCredentials("admin", ""),
                    existingProfile.id,
                )
            }
            coVerify { saveServerProfile(match { it.id == existingProfile.id }, null) }
        }
    }

    @Test
    fun connectionEditorViewModelBehavesLikeNewServerWhenTheProfileToEditDoesNotExist() = runTest {
        turbineScope {
            coEvery { getServerProfile(existingProfile.id) } returns null

            val viewStateTurbine = createViewModel(profileId = "alpha").viewState.testIn(this)
            runCurrent()

            assertEquals(ViewState(), viewStateTurbine.cancelAndConsumeItems().last())
        }
    }

    @Test
    fun connectionEditorViewModelCancelsARunningTestWhenTheFormIsEdited() = runTest {
        turbineScope {
            val events = MutableSharedFlow<Event>()
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns events

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()
            events.emit(Event.StepStarted(Step.ReachHost))
            runCurrent()
            assertEquals(1, events.subscriptionCount.value)

            viewModel.onViewAction(OnHostChange("other.local"))
            runCurrent()
            assertEquals(0, events.subscriptionCount.value)
            events.emit(Event.Succeeded(GlancesApiVersion.V4))
            runCurrent()

            val finalState = viewStateTurbine.cancelAndConsumeItems().last()
            assertEquals(Phase.Idle, finalState.phase)
            assertEquals("other.local", finalState.host)
            assertEquals(checks(Pending, Pending, Pending), finalState.checks)
            coVerify(exactly = 0) { saveServerProfile(any(), any()) }
        }
    }

    @Test
    fun connectionEditorViewModelIgnoresSubmitWhileATestIsRunning() = runTest {
        turbineScope {
            val events = MutableSharedFlow<Event>()
            every { validateServerInput(any(), any(), any()) } returns validInput
            every { testConnection(any(), any(), any()) } returns events

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()
            viewModel.onViewAction(OnSubmit)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            verify(exactly = 1) { testConnection(any(), any(), any()) }
        }
    }

    private fun checks(reach: ConnectionCheckStatusUi, detect: ConnectionCheckStatusUi, authenticate: ConnectionCheckStatusUi) =
        listOf(
            ConnectionCheckUi(ConnectionCheckStepUi.ReachHost, reach),
            ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances, detect),
            ConnectionCheckUi(ConnectionCheckStepUi.Authenticate, authenticate),
        )

    companion object {
        private val Pending = ConnectionCheckStatusUi.Pending
        private val Running = ConnectionCheckStatusUi.Running
        private val Passed = ConnectionCheckStatusUi.Passed
        private val Failed = ConnectionCheckStatusUi.Failed

        private val validInput = ValidateServerInputUseCase.Result(alias = null, host = null, port = null)
        private val invalidInput = ValidateServerInputUseCase.Result(
            alias = AliasError.Blank,
            host = HostError.ContainsScheme,
            port = PortError.Blank,
        )

        private val existingProfile = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Https, "alpha.local", 8443),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.Basic("admin"),
        )

        private fun successfulTest(apiVersion: GlancesApiVersion) = flowOf(
            Event.StepStarted(Step.ReachHost),
            Event.StepPassed(Step.ReachHost),
            Event.StepStarted(Step.DetectGlances),
            Event.StepPassed(Step.DetectGlances),
            Event.StepStarted(Step.Authenticate),
            Event.StepPassed(Step.Authenticate),
            Event.Succeeded(apiVersion),
        )

        private fun failedTest(step: Step, failure: ConnectionFailure) = flowOf(
            Event.StepStarted(step),
            Event.Failed(step, failure),
        )
    }
}
