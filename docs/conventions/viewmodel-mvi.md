# ViewModel & MVI Architecture

Nascope uses Model-View-Intent (MVI) unidirectional data flow built with AndroidX ViewModel and Kotlin Coroutines/Flow.

---

## 1. Brick Autonomy & Shared State Holders

- Every dashboard brick has its own dedicated `@KoinViewModel` (e.g. `SystemOverviewViewModel`).
- **No data pass-down**: The dashboard container does not supply telemetry or business data to bricks.
- **Shared state holders**: Brick ViewModels collect domain and telemetry streams directly from shared state holders (e.g. `TelemetryStateHolder`) injected via Koin constructor injection.

---

## 1a. UI Models (no data models in ViewState)

- Data-layer models (`io.buszi.nascope.data.*`) must **never** appear in a `ViewState`, `ViewAction`, composable signature or preview. They are destined for a separate module.
- ViewModels map data models to UI models at the boundary using a `from(data)` factory in the UI model's companion object, e.g. `storageData.mounts.map(StorageMountUi::from)`. UI models live in `ui/common/model` so they can be reused across widgets and details screens.
- Derived values and status rules (usage ratio, `StatusLevel`, gauge progress, thresholds) are members of the UI model, not free helper functions over data models.

---

## 2. MVI Component Structure

Each ViewModel defines three distinct components:

### A. ViewState
- Single immutable `@Stable data class ViewState(...)`.
- Defines all fields required by the UI with sensible defaults reflecting the initial state.
- Must include a `widgetState: WidgetState = WidgetState.Loading` property to represent lifecycle status (`Loading`, `Ready`, `Empty`, `Error`).

### B. ViewAction
- Sealed interface declaring all user-initiated UI events:
  ```kotlin
  sealed interface ViewAction {
      data object OnRetry : ViewAction
      data class OnSelectDisk(val diskId: String) : ViewAction
  }
  ```

### C. Action Handler (`onViewAction`)
- A single public entry point: `fun onViewAction(action: ViewAction)`.
- Handles intents cleanly by dispatching to private methods.

---

## 3. StateFlow Exposure & Coroutine Scopes

- Maintain an internal mutable state flow:
  ```kotlin
  private val mutableViewState = MutableStateFlow(ViewState())
  val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()
  ```
- Collect domain flows and background telemetry within `viewModelScope` using `.launchIn(viewModelScope)`:
  ```kotlin
  private fun observeTelemetry() {
      telemetryStateHolder.hostOverview
          .onEach { overview ->
              mutableViewState.update { current ->
                  current.copy(
                      hostname = overview.hostname,
                      widgetState = WidgetState.Ready,
                  )
              }
          }
          .launchIn(viewModelScope)
  }
  ```
- Always use `mutableViewState.update { ... }` to ensure thread-safe atomic updates.
