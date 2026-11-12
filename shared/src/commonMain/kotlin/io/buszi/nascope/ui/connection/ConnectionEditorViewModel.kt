package io.buszi.nascope.ui.connection

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.GetServerProfileUseCase
import io.buszi.nascope.domain.connection.SaveServerProfileUseCase
import io.buszi.nascope.domain.connection.TestConnectionUseCase
import io.buszi.nascope.domain.connection.TestConnectionUseCase.Event
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.ui.common.model.ConnectionCheckStatusUi
import io.buszi.nascope.ui.common.model.ConnectionCheckStepUi
import io.buszi.nascope.ui.common.model.ConnectionCheckUi
import io.buszi.nascope.ui.common.model.ConnectionFailureUi
import io.buszi.nascope.ui.common.model.FormErrorUi
import io.buszi.nascope.ui.common.model.ServerProtocolUi
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnAliasChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnAuthToggle
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnHostChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnPasswordChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnPortChange
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnProtocolSelect
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnSubmit
import io.buszi.nascope.ui.connection.ConnectionEditorViewModel.ViewAction.OnUsernameChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@KoinViewModel
class ConnectionEditorViewModel(
    @InjectedParam profileId: String?,
    private val validateServerInput: ValidateServerInputUseCase,
    private val testConnection: TestConnectionUseCase,
    private val saveServerProfile: SaveServerProfileUseCase,
    private val getServerProfile: GetServerProfileUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState(isLoading = profileId != null, isEditMode = profileId != null))
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    private var editedProfileId: ServerProfileId? = profileId?.let(::ServerProfileId)
    private var testJob: Job? = null
    private val editedFields = mutableSetOf<Field>()
    private var submitted = false

    init {
        editedProfileId?.let(::loadProfile)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnAliasChange -> edit(Field.Alias) { copy(alias = action.value) }
            is OnProtocolSelect -> edit(Field.Protocol) { copy(protocol = action.protocol) }
            is OnHostChange -> edit(Field.Host) { copy(host = action.value) }
            is OnPortChange -> edit(Field.Port) { copy(port = action.value) }
            is OnAuthToggle -> edit(Field.Auth) { copy(authEnabled = action.enabled) }
            is OnUsernameChange -> edit(Field.Username) { copy(username = action.value) }
            is OnPasswordChange -> edit(Field.Password) { copy(password = action.value) }
            OnSubmit -> submit()
        }
    }

    private fun loadProfile(id: ServerProfileId) {
        viewModelScope.launch {
            val profile = getServerProfile(id)
            if (profile == null) {
                // Nothing to edit: behave like a new server.
                editedProfileId = null
                mutableViewState.update { it.copy(isLoading = false, isEditMode = false) }
                return@launch
            }
            val auth = profile.auth
            mutableViewState.update {
                it.copy(
                    isLoading = false,
                    alias = profile.alias,
                    protocol = ServerProtocolUi.from(profile.address.protocol),
                    host = profile.address.host,
                    port = profile.address.port.toString(),
                    authEnabled = auth is ServerAuth.Basic,
                    username = (auth as? ServerAuth.Basic)?.username ?: DefaultUsername,
                )
            }
        }
    }

    /** Applies a field change, drops any running test and its results, and refreshes the errors. */
    private fun edit(field: Field, change: ViewState.() -> ViewState) {
        if (!mutableViewState.value.isEditable) return
        testJob?.cancel()
        testJob = null
        editedFields += field
        mutableViewState.update {
            it.change()
                .copy(
                    phase = Phase.Idle,
                    checks = ConnectionCheckUi.initial,
                    showSecretNotPersistedNotice = false,
                )
                .withErrors()
        }
    }

    private fun submit() {
        if (!mutableViewState.value.canSubmit) return
        submitted = true
        mutableViewState.update { it.withErrors() }
        val state = mutableViewState.value
        if (state.hasErrors) return

        mutableViewState.update { it.copy(phase = Phase.Testing, checks = ConnectionCheckUi.initial) }
        testJob = viewModelScope.launch {
            val address = ServerAddress(state.protocol.toDomain(), state.host.trim(), state.port.trim().toInt())
            val credentials = if (state.authEnabled) ServerCredentials(state.username.trim(), state.password) else null
            testConnection(address, credentials, editedProfileId).collect(::onTestEvent)
        }
    }

    private suspend fun onTestEvent(event: Event) {
        when (event) {
            is Event.StepStarted -> updateCheck(event.step) { it.copy(status = ConnectionCheckStatusUi.Running) }
            is Event.StepPassed -> updateCheck(event.step) { it.copy(status = ConnectionCheckStatusUi.Passed) }
            is Event.Failed -> onTestFailed(event)
            is Event.Succeeded -> save(event.apiVersion)
        }
    }

    private fun onTestFailed(event: Event.Failed) {
        val failure = ConnectionFailureUi.from(event.failure)
        updateCheck(event.step) { it.copy(status = ConnectionCheckStatusUi.Failed, failure = failure) }
        mutableViewState.update { state ->
            val needsCredentials = failure == ConnectionFailureUi.Unauthorized && !state.authEnabled
            state.copy(
                phase = Phase.Idle,
                authEnabled = state.authEnabled || needsCredentials,
                usernameFocusRequest = if (needsCredentials) state.usernameFocusRequest + 1 else state.usernameFocusRequest,
            )
        }
    }

    private suspend fun save(apiVersion: GlancesApiVersion) {
        val state = mutableViewState.value
        mutableViewState.update { it.copy(phase = Phase.Saving) }

        val id = editedProfileId ?: ServerProfileId(Uuid.random().toString())
        val profile = ServerProfile(
            id = id,
            alias = state.alias.trim(),
            address = ServerAddress(state.protocol.toDomain(), state.host.trim(), state.port.trim().toInt()),
            apiVersion = apiVersion,
            auth = if (state.authEnabled) ServerAuth.Basic(state.username.trim()) else ServerAuth.None,
        )
        // An empty password keeps the stored secret, a typed one replaces it.
        val password = state.password.takeIf { state.authEnabled && it.isNotEmpty() }

        // Saving must not be torn apart by a cancellation: the secret and the profile are written together.
        val result = withContext(NonCancellable) { saveServerProfile(profile, password) }

        editedProfileId = id
        mutableViewState.update {
            it.copy(
                phase = Phase.Saved,
                showSecretNotPersistedNotice = state.authEnabled && !result.secretPersisted,
            )
        }
    }

    private fun updateCheck(step: TestConnectionUseCase.Step, change: (ConnectionCheckUi) -> ConnectionCheckUi) {
        val stepUi = ConnectionCheckStepUi.from(step)
        mutableViewState.update { state ->
            state.copy(checks = state.checks.map { if (it.step == stepUi) change(it) else it })
        }
    }

    /** Recomputes the field errors; those of fields that were not edited yet stay hidden until a submit. */
    private fun ViewState.withErrors(): ViewState {
        val result = validateServerInput(alias.trim(), host.trim(), port.trim())
        return copy(
            aliasError = result.alias?.let(FormErrorUi::from).shownFor(Field.Alias),
            hostError = result.host?.let(FormErrorUi::from).shownFor(Field.Host),
            portError = result.port?.let(FormErrorUi::from).shownFor(Field.Port),
            usernameError = FormErrorUi.UsernameBlank
                .takeIf { authEnabled && username.isBlank() }
                .shownFor(Field.Username),
        )
    }

    private fun FormErrorUi?.shownFor(field: Field): FormErrorUi? =
        takeIf { submitted || field in editedFields }

    private enum class Field { Alias, Protocol, Host, Port, Auth, Username, Password }

    @Stable
    data class ViewState(
        val alias: String = "",
        val protocol: ServerProtocolUi = ServerProtocolUi.Http,
        val host: String = "",
        val port: String = DefaultPort,
        val authEnabled: Boolean = false,
        val username: String = DefaultUsername,
        val password: String = "",
        val aliasError: FormErrorUi? = null,
        val hostError: FormErrorUi? = null,
        val portError: FormErrorUi? = null,
        val usernameError: FormErrorUi? = null,
        val checks: List<ConnectionCheckUi> = ConnectionCheckUi.initial,
        val phase: Phase = Phase.Idle,
        val isEditMode: Boolean = false,
        val isLoading: Boolean = false,
        /** Increments whenever the username field should take focus, e.g. after the server asked for credentials. */
        val usernameFocusRequest: Int = 0,
        val showSecretNotPersistedNotice: Boolean = false,
    ) {
        /** Whether the fields accept input. Editing while a test runs is allowed and cancels that test. */
        val isEditable: Boolean
            get() = !isLoading && (phase == Phase.Idle || phase == Phase.Testing)

        /** Whether the submit action can start a new test. */
        val canSubmit: Boolean
            get() = !isLoading && phase == Phase.Idle

        val hasErrors: Boolean
            get() = aliasError != null || hostError != null || portError != null || usernameError != null
    }

    /** Where the submit flow currently is. */
    enum class Phase {
        Idle,
        Testing,
        Saving,
        Saved,
    }

    sealed interface ViewAction {
        data class OnAliasChange(val value: String) : ViewAction
        data class OnProtocolSelect(val protocol: ServerProtocolUi) : ViewAction
        data class OnHostChange(val value: String) : ViewAction
        data class OnPortChange(val value: String) : ViewAction
        data class OnAuthToggle(val enabled: Boolean) : ViewAction
        data class OnUsernameChange(val value: String) : ViewAction
        data class OnPasswordChange(val value: String) : ViewAction
        data object OnSubmit : ViewAction
    }

    companion object {
        const val DefaultPort: String = "61208"
        const val DefaultUsername: String = "glances"
    }
}
