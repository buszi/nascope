# Widget & Screen Composition Conventions

Rules for composing dashboard bricks and screens into clean, decoupled, previewable UI units.

---

## 1. Stateful vs. Stateless Split

Every widget brick consists of two composable functions:

### A. Stateful Entrypoint
- Injects the ViewModel via Koin: `viewModel: MyViewModel = koinViewModel()`.
- Collects `viewState` with lifecycle awareness:
  ```kotlin
  val viewState by viewModel.viewState.collectAsStateWithLifecycle()
  ```
- Forwards user actions to the ViewModel: `onViewAction = viewModel::onViewAction`.
- Contains **no layout or visual rendering logic** itself.

```kotlin
@Composable
fun SystemOverviewWidget(
    modifier: Modifier = Modifier,
    viewModel: SystemOverviewViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    SystemOverviewContent(
        viewState = viewState,
        onViewAction = viewModel::onViewAction,
        modifier = modifier,
    )
}
```

### B. Stateless Content Composable (Pure UI)
- Takes `viewState: ViewState`, `onViewAction: (ViewAction) -> Unit`, and `modifier: Modifier = Modifier`.
- Completely decoupled from Koin and ViewModels.
- Readily previewable in `@Preview` and testable in Compose UI tests.

```kotlin
@Composable
fun SystemOverviewContent(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TelemetryCard(modifier = modifier) {
        WidgetHeader(
            title = stringResource(Res.string.system_overview_title),
            widgetState = viewState.widgetState,
            onRetry = { onViewAction(ViewAction.OnRetry) },
        )
        // ... brick-specific telemetry displays ...
    }
}
```

---

## 2. Visual Structure & Packaging

1. **Card Container**: Wrap every brick in `TelemetryCard` (layer 1 surface token `subSurface` with `baseBorder`).
2. **Standardized Header**: Include `WidgetHeader` at the top of every brick:
   - Title string from Compose resources (`Res.string.*`).
   - Status badge displaying `WidgetState` (`Loading`, `Ready`, `Stale`, `Unavailable`, `Offline`, `Error`).
   - Contextual action icon buttons (e.g. Refresh/Retry, Expand, Menu).
3. **Design System Metrics**: Build all telemetry visual displays exclusively from design tokens and common components:
   - `MetricValue` for tabular metrics and readouts.
   - `TelemetryProgressBar` for percentage capacities and load bars.
   - `StatusChip` / `TagChip` for state badges and tags.

---

## Widget states

| `WidgetState` | Meaning | `DashboardWidget` renders |
| :--- | :--- | :--- |
| `Loading` | No value yet (first load, server switch, long background pause) | skeleton |
| `Ready` | Fresh telemetry | content |
| `Stale` (`isReady = true`) | The latest poll failed; values are from an earlier one | content plus a `STALE` tag in the header |
| `Unavailable` | The host has no data for this brick (plugin missing or disabled) | muted "Not available on this host", no retry |
| `Offline` | Connection failure, with the connection failure message | offline panel with reconnect |
| `Error` | The server answered with unreadable data | error panel with retry |

- `TelemetryState.toWidgetState(onRetry)` is the only mapping from a use case result to these states.
- `DetailsScaffold(widgetState = ...)` shows the stale tag; details screens use `DetailsStatePlaceholder(state)` instead of the sections while `!isReady`.
- A value the host may not provide (`null` in the domain model) is either left out (tag, section) or shown as the `widget_state_value_placeholder` dash; the same choice is made in the brick and its details screen.
