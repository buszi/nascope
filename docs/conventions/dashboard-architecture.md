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

- The bricks shown and their order are decided by `DashboardViewModel` from three independent inputs: the registered bricks (`BrickRegistry.allBricks`), the user's stored layout (`ObserveDashboardLayoutUseCase`, a global `DashboardLayout` of brick keys) and the server's plugins (`ObserveAvailablePluginsUseCase`):
  ```kotlin
  combine(observeDashboardLayout(), observeAvailablePlugins()) { layout, plugins ->
      brickRegistry.allBricks.arrangedBy(layout).enabledIn(layout).visibleWith(plugins)
  }
  ```
- **Shown = registered and turned on and available**, in the layout's order. The inputs stay independent: turning a brick off does not touch availability, and an unavailable brick keeps its place and its switch for the day the server offers it.
- **Layout keys:** the layout names bricks by `DashboardBrickId.key` (`DashboardBrick.key`), an explicit, stable string such as `"cpu"` or `"storage"`, never the enum `name`, so renaming an entry keeps users' layouts. `DashboardBrickKeyTest` pins every key; never change one.
- **Resolving a layout** (`framework/BrickArrangement.kt`, pure): `arrangedBy(layout)` returns every registered brick in the layout's order. A brick the order does not name (added by a later app version, or every brick for `DashboardLayout.Default`) goes right after the nearest brick before it in the registry order that the result already holds, or first when there is none, so `Default` is exactly the registry order. A key no brick has is skipped; a key named twice counts where it is first named. `enabledIn(layout)` drops the bricks in `layout.hidden`, so a brick not named there (a new one) is on.
- **Not loaded yet:** `ViewState.bricks` is `null` until the first layout emission (a local read), so the default layout never flashes before a custom one; the screen then shows only the header. An empty list (every brick off, or none left after availability) shows the dashboard's empty state (`TestTags.DashboardEmpty`), whose button opens the editor.
- **Editor** (`ui/dashboard/customize/`): `NavRoute.DashboardCustomize` is a detail pane like `Details` (beside the dashboard on Medium / Expanded, full screen on Compact). The header's customize action toggles it, and it and brick details replace each other in the one pane entry above the dashboard. Its rows come from `DashboardCustomizeUi.from(registry.allBricks, layout, plugins)`, which reuses `arrangedBy`, `enabledIn` and `visibleWith`, so the editor never disagrees with the dashboard. Every edit writes the complete layout (`toLayout()`) at once, applied to the ViewModel's state first so a second edit made before the store answers builds on it. "Reset to default" writes `DashboardLayout.Default` after an inline confirmation and is disabled while the layout is `Default`.
- **Plugin-driven visibility:** `DashboardBrick.requiredPlugins` (default empty = always shown) names the Glances plugins a brick needs, e.g. `GpuBrick` declares `setOf("gpu")`. `visibleWith(plugins)` shows a brick when the server offers all its plugins. While the plugins are unknown (loading, failed, server switched) every brick that is on is shown, so each brick's own widget state handles the failure.
- A brick that is turned off is not composed, so its ViewModel is not created and its plugins are not polled (a polling loop exists only while collected).
- **Process details, programs:** where the server lists `programlist` (Glances 4; the toggle stays hidden while the plugin list is unknown or lacks it) a PROCESSES / PROGRAMS toggle switches the table to processes grouped by program (process count, summed CPU judged per machine, memory; expanded: user, threads, RSS). Search text, sort and paging are shared; the sort falls back to CPU when its column (PID or PROCS) does not exist in the other mode. There is no state filter and no pinning in PROGRAMS mode, and pins (by PID) are untouched. The programs are collected only while PROGRAMS is selected.
- **Storage details, disks:** a DISKS section lists each disk Glances reports, busiest (read + write) first, five per page (`DetailsPager`), each with read / write rate and sparklines, IOPS and latency when the API reports them; it is left out when `diskio` is unavailable or empty. The brick keeps the summed throughput. Partitions and virtual devices (zram, loop, nbd, device mapper, eMMC boot areas) are left out of the list (`withoutPartitionsAndVirtualDevices`; kept when nothing else is left), because a NAS reports dozens and their traffic is the disk's again; arrays (`md`) and caches (`bcache`) stay. The brick's summed figure still counts every device the host reports, so a write is counted once per layer.
- **Alerts:** `AlertsBrick` (`requiredPlugins = setOf("alert")`, 2 columns) lists the active Glances alerts, worst first, each with its readable metric (`MEM` -> MEMORY, unknown types as Glances names them), description and running time, or "ALL CLEAR". Glances has three severities and the app three status levels, so careful and warning share the degraded colour and the chip text tells them apart. Its details show the active alerts and the recent history with min / avg / max, top processes and the time each lasted. The dashboard header shows an "N ALERTS" chip in the worst colour; `DashboardViewModel` only collects `ObserveAlertsUseCase` once the plugin list says the server runs `alert`. The chip does not depend on the Alerts brick and stays when the brick is turned off. There is no "clear alerts" action yet (see TASK-6A.2).
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
- **Registry order is the default order:** `BrickRegistry.allBricks` lists every brick once, and its order is what `DashboardLayout.Default` shows and where a brick missing from a stored layout is placed. Add a new brick next to the bricks it belongs beside. `findBrick(id)` finds a brick whether it is turned on or not.
- **A new brick** needs a new `DashboardBrickId` entry with its own new `key` (pinned in `DashboardBrickKeyTest`); it is on by default for every user.

### `BrickSpan`
- `BrickSpan.Fixed(val cells: Int)`: The brick requests an exact number of horizontal cells (e.g. 1 or 2).
- `BrickSpan.FullRow`: The brick stretches across all columns in the current row (`GridItemSpan(maxLineSpan)`).

### Zero Empty Spaces Policy
- The visible bricks and their order vary (the user's layout and "Plugin-driven visibility", section 3), so no brick count or order can be relied on. `computeBrickSpans(spans, columns)` (`framework/BrickLayout.kt`, pure, unit-tested at 2, 3 and 4 columns for every subset of the registered bricks, every order of up to five of them and seeded random orders of all and of some) turns the bricks' `BrickSpan`s into cell counts and `AdaptiveDashboardGrid` uses them: a brick takes the cells its span asks for, at most what is left of its row; `BrickSpan.FullRow` is a row of its own (an unfinished row before it is completed first); a row that ends unfinished is completed by widening its bricks, starting with the last. Adding a brick needs no layout arithmetic.
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
