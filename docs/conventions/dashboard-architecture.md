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
  ) : ViewModel() {
      private val mutableViewState = MutableStateFlow(
          ViewState(bricks = brickRegistry.getActiveBricks()),
      )
      val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()
  }
  ```
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
- Bricks with details: CPU Matrix, Containers, Process List, Storage Pools, Network I/O and Sensors & Thermals. System Overview and Memory & Swap have none because their cards already show all their data. Add details to a brick only when they show more than the card does.
- If a `Details` route points at a brick without details (e.g. restored state), `NascopeAppScaffold` pops it immediately.
- A details ViewModel follows the same MVI rules as widget ViewModels and owns its own UI state (sort, filter, expanded rows); data comes from `TelemetryStateHolder`.
