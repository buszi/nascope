# Dashboard & Brick Architecture

Nascope's dashboard is built on a modular "brick" architecture rendered inside a `LazyVerticalGrid`.

---

## 1. LazyVerticalGrid Foundation

The dashboard is structured around a single `LazyVerticalGrid` that dynamically adapts its horizontal column count to the window size:

- **Compact** (`< 768.dp`): **2 columns** (mobile form factor).
- **Medium** (`768.dp .. 1280.dp`): **3 columns** (tablet / medium desktop).
- **Expanded** (`>= 1280.dp`): **4 columns** (desktop / widescreen dashboard canvas).
- **Ultra-wide** (`> 1600.dp`): 4 grid columns, with remaining horizontal canvas space reserved for a right-side brick detail panel (implemented in the Navigation3 phase).

Breakpoints must be evaluated using `WindowWidthClass.fromWidth(maxWidth)` rather than hardcoding numeric breakpoint dp values in screen composables.

---

## 2. Container Role of DashboardScreen

`DashboardScreen` is solely an orchestrator and layout container:
- It **never** previews, directly references, or renders the internal layout of any brick.
- It observes `DashboardViewModel.viewState.bricks` and renders each active brick through its public contract:
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

### `BrickSpan`
- `BrickSpan.Fixed(val cells: Int)`: The brick requests an exact number of horizontal cells (e.g. 1 or 2).
- `BrickSpan.FullRow`: The brick stretches across all columns in the current row (`GridItemSpan(maxLineSpan)`).

### Zero Empty Spaces Policy
- Bricks must adapt their span to `WindowWidthClass` to avoid awkward empty cells or unbalanced gaps at the end of rows.
- If a row has leftover space or a brick contains expansive data (e.g., storage pool breakdowns or network traffic timelines), it should report `FullRow` or an expanded cell span for wider form factors.
