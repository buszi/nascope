# ViewModel & MVI Architecture

Nascope uses Model-View-Intent (MVI) unidirectional data flow built with AndroidX ViewModel and Kotlin Coroutines/Flow.

---

## 1. Brick Autonomy & Shared State Holders

- Every dashboard brick has its own dedicated `@KoinViewModel` (e.g. `SystemOverviewViewModel`).
- **No data pass-down**: The dashboard container does not supply telemetry or business data to bricks.
- **Observing use cases**: A brick ViewModel injects the `Observe<Feature>UseCase` interface from `:domain` (a `() -> Flow<TelemetryState<Data>>`) and `RefreshTelemetryUseCase`. A brick and its details ViewModel use the same use case. (`TelemetryStateHolder` is the mock-data holder that is being replaced brick by brick during Phase 6; do not use it in new code.)
- **Widget state**: the ViewModel never builds `WidgetState` by hand from a use case result. It calls the single mapping function `TelemetryState.toWidgetState(onRetry)` (next to `WidgetState`), which yields `Loading`, `Ready`, `Stale`, `Unavailable`, `Offline` or `Error`. Retry in a brick calls `RefreshTelemetryUseCase`.

---

## 1a. UI Models (no data models in ViewState)

- Data-layer models (`io.buszi.nascope.data.*`) and domain models (`io.buszi.nascope.domain.model.*`, plus models nested in use case interfaces) must **never** appear in a `ViewState`, `ViewAction`, composable signature or preview.
- ViewModels map data models to UI models at the boundary using a `from(data)` factory in the UI model's companion object, e.g. `storageData.mounts.map(StorageMountUi::from)`. UI models live in `ui/common/model` so they can be reused across widgets and details screens.
- Derived values and status rules (usage ratio, `StatusLevel`, gauge progress, thresholds) are members of the UI model, not free helper functions over data models.

---

## 2. MVI Component Structure

Each ViewModel defines three distinct components:

### A. ViewState
- Single immutable `@Stable data class ViewState(...)`.
- Defines all fields required by the UI with sensible defaults reflecting the initial state.
- Must include a `widgetState: WidgetState = WidgetState.Loading` property to represent lifecycle status (`Loading`, `Ready`, `Stale`, `Unavailable`, `Offline`, `Error`).

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
      observeSystemOverview()
          .onEach { state ->
              mutableViewState.update { current ->
                  current.copy(
                      overview = when (state) {
                          is TelemetryState.Ready -> SystemOverviewUi.from(state.data)
                          TelemetryState.Loading -> SystemOverviewUi()
                          is TelemetryState.Failed -> current.overview
                      },
                      widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                  )
              }
          }
          .launchIn(viewModelScope)
  }
  ```
- Always use `mutableViewState.update { ... }` to ensure thread-safe atomic updates.
