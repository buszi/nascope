# Dashboard & Brick Architecture

Nascope's dashboard is built on a modular "brick" architecture rendered inside a `LazyVerticalGrid`.

---

## 1. LazyVerticalGrid Foundation

The dashboard is structured around a single `LazyVerticalGrid`, owned by the `AdaptiveDashboardGrid` composable, that adapts its columns, margins and gutters to the width class of the space it is laid out in. The values live in `DashboardGridMetrics.forWidthClass` and come from `AppSpacing` tokens:

| Class | Width | Columns | Gutter | Margin | Brick heights |
|---|---|---|---|---|---|
| **Compact** | `< 768dp` | 2 | 12dp (`spaceMd`) | 16dp (`margin`) | natural (wrap content) |
| **Medium** | `768dp .. 1279dp` | 3 | 16dp (`gutter`) | 24dp (`marginTablet`) | modular 1U steps |
| **Expanded** | `>= 1280dp` | 4 | 20dp (`gutterDesktop`) | 32dp (`marginDesktop`) | modular 1U steps |

- The class is derived from the grid's own width (plus the navigation rail), not the window, so opening the details inspector beside the dashboard reflows it to the narrower layout.
- The brick height unit resolves with the grid's gutter (`BrickHeight.resolve(unit, gutter)`), so a 2U brick always lines up with two stacked 1U bricks.
- Bricks claim cells via `DashboardBrick.span`; add new layouts by changing the metrics, never by hardcoding dp values in screens.

Breakpoints must be evaluated using `WindowWidthClass.fromWidth(maxWidth)` rather than hardcoding numeric breakpoint dp values in screen composables.

---

## 2. Container Role of DashboardScreen

`DashboardScreen` is solely an orchestrator and layout container:
- It **never** previews, directly references, or renders the internal layout of any brick.
- It observes `DashboardViewModel.viewState.bricks` and hands them to `AdaptiveDashboardGrid`, which renders each active brick through its public contract:
  ```kotlin
  items(
      items = viewState.bricks,
      key = { it.id.value },
      span = { brick ->
          when (val span = brick.cellsSpan(windowWidthClass)) {
              is BrickSpan.Fixed -> GridItemSpan(min(span.cells, maxLineSpan))
              BrickSpan.FullRow -> GridItemSpan(maxLineSpan)
          }
      },
  ) { brick ->
      brick.Content(modifier = Modifier.fillMaxWidth())
  }
  ```
- This ensures zero coupling between the dashboard container and widget-specific ViewModels, domain flows, or state models.

---

## 3. Active Bricks & Ordering

- The set of active bricks and their order is controlled by `DashboardViewModel`.
- `DashboardViewModel` queries a local configuration repository (`BrickRegistry`):
  ```kotlin
  @KoinViewModel
  class DashboardViewModel(
      brickRegistry: BrickRegistry,
      observeSession: ObserveSessionUseCase,
      observeConnectionStatus: ObserveConnectionStatusUseCase,
      private val refreshTelemetry: RefreshTelemetryUseCase,
  ) : ViewModel() {
      private val mutableViewState = MutableStateFlow(
          ViewState(bricks = brickRegistry.getActiveBricks()),
      )
      val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()
  }
  ```
- **Plugin-driven visibility:** `DashboardBrick.requiredPlugins` (default empty = always shown) names the Glances plugins a brick needs, e.g. `GpuBrick` declares `setOf("gpu")`. `DashboardViewModel` collects `ObserveAvailablePluginsUseCase` and exposes `brickRegistry.getActiveBricks().visibleWith(plugins)`: a brick is shown when the server offers all its plugins. While the plugins are unknown (loading, failed, server switched) every brick is shown, so each brick's own widget state handles the failure. Phase 7's user toggles must compose with this: shown = available and not turned off.
- **Alerts:** `AlertsBrick` (`requiredPlugins = setOf("alert")`, 2 columns) lists the active Glances alerts, worst first, each with its readable metric (`MEM` -> MEMORY, unknown types as Glances names them), description and running time, or "ALL CLEAR". Glances has three severities and the app three status levels, so careful and warning share the degraded colour and the chip text tells them apart. Its details show the active alerts and the recent history with min / avg / max, top processes and the time each lasted. The dashboard header shows an "N ALERTS" chip in the worst colour; `DashboardViewModel` only collects `ObserveAlertsUseCase` once the plugin list says the server runs `alert`. There is no "clear alerts" action yet (see TASK-6A.2).
- Beyond the bricks, the dashboard level only knows the active server alias, its live connection status (`SessionStatusUi`: none / demo / server + `ConnectionStatusUi`) and a refresh action (`RefreshTelemetryUseCase`). It never owns telemetry, widget states or error banners: every brick maps its own `TelemetryState` to a `WidgetState`. The header chip and the sidebar rail dot follow the connection status, and are `DEMO` / neutral in demo mode.
- Adding, removing, or re-ordering bricks does not require changing `DashboardScreen`.

---

## 4. The Brick Contract (`DashboardBrick`)

Every widget on the dashboard must implement `DashboardBrick`:

```kotlin
interface DashboardBrick {
    val id: DashboardBrickId
    val titleRes: StringResource

    @Composable
    fun Content(modifier: Modifier)

    fun cellsSpan(windowWidthClass: WindowWidthClass): BrickSpan
}
```

- **Brick Implementations**: Bricks are stateless descriptor definitions declared as `data object` (e.g. `data object SystemOverviewBrick : DashboardBrick`). Because Kotlin objects are language-level singletons, they must **not** be annotated with Koin's `@Single` (which would cause Koin to attempt constructor reflection on a synthetic private constructor). `BrickRegistry` provides them directly.

### `BrickSpan`
- `BrickSpan.Fixed(val cells: Int)`: The brick requests an exact number of horizontal cells (e.g. 1 or 2).
- `BrickSpan.FullRow`: The brick stretches across all columns in the current row (`GridItemSpan(maxLineSpan)`).

### Zero Empty Spaces Policy
- The visible bricks vary (see "Plugin-driven visibility" below), so no brick count can be relied on. `computeBrickSpans(spans, columns)` (`framework/BrickLayout.kt`, pure, unit-tested for every subset of the registered bricks at 2, 3 and 4 columns) turns the bricks' `BrickSpan`s into cell counts and `AdaptiveDashboardGrid` uses them: a brick takes the cells its span asks for, at most what is left of its row; `BrickSpan.FullRow` is a row of its own (an unfinished row before it is completed first); a row that ends unfinished is completed by widening its bricks, starting with the last. Adding a brick needs no layout arithmetic.
- Bricks must adapt their span to `WindowWidthClass` to avoid awkward empty cells or unbalanced gaps at the end of rows.
- If a row has leftover space or a brick contains expansive data (e.g., storage pool breakdowns or network traffic timelines), it should report `FullRow` or an expanded cell span for wider form factors.

---

## 5. Brick Details (Drill-Down)

A brick opts into drill-down by overriding `details: BrickDetails?` (default `null`):

```kotlin
override val details: BrickDetails = BrickDetails { onClose, modifier ->
    StorageDetailsScreen(onClose = onClose, modifier = modifier)
}
```

- Each brick owns a **separate** details screen + ViewModel in its own package. Details are only added when they show more than the brick already shows.
- `NascopeAppScaffold` resolves `NavRoute.Details(brickId)` through `BrickRegistry.findBrick(id)?.details`; the same composable fills the 340–380dp inspector pane on Medium/Expanded and the whole screen on Compact.
- `DashboardScreen` only attaches a click handler to bricks that have details.
- Affordance: `DashboardScreen` provides `LocalBrickInteraction` (`None` / `Openable` / `Open`) around each brick and `DashboardWidget` renders it: a trailing `DetailsGlyph` (↗) when openable, plus an accent (`telemetryA`) border and raised fill while the brick's details are open. Bricks must use `DashboardWidget` as their root for this to apply.

### Details screen building blocks (`ui/details/components`)
- `DetailsScaffold`: shared frame (header with title/subtitle/status tag/close action, scrollable body). The close action is hidden on Compact, where the top bar provides back navigation.
- `DetailsSection`: titled `TelemetryCard` grouping one aspect of the details; `DetailsKeyValueRow` for property readouts; `DetailsStat` for label-over-value metric readouts; `DetailsPager` for paginated lists; `DetailsLoadingPlaceholder` for the loading state.
- `SelectableChip` (`ui/common/components/inputs`): single-choice toggle for sort orders and filters.
- Bricks with details: CPU Matrix, Containers, Process List, Storage Pools, Network I/O, Sensors & Thermals and Alerts. System Overview, Memory & Swap and GPU have none because their cards already show all their data. The GPU brick (`gpu` plugin) shows an empty-state message on a host without a GPU Glances can read, and "unavailable" when the plugin is missing. Add details to a brick only when they show more than the card does.
- If a `Details` route points at a brick without details (e.g. restored state), `NascopeAppScaffold` pops it immediately.
- A details ViewModel follows the same MVI rules as widget ViewModels and owns its own UI state (sort, filter, expanded rows); data comes from the same `Observe<Feature>UseCase` as the brick, so the brick and its details screen show one set of values and one history.
