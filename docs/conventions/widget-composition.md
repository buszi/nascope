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
   - Status badge displaying `WidgetState` (`Loading`, `Ready`, `Empty`, `Error`).
   - Contextual action icon buttons (e.g. Refresh/Retry, Expand, Menu).
3. **Design System Metrics**: Build all telemetry visual displays exclusively from design tokens and common components:
   - `MetricValue` for tabular metrics and readouts.
   - `TelemetryProgressBar` for percentage capacities and load bars.
   - `StatusChip` / `TagChip` for state badges and tags.
