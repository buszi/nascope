# Nascope Code Conventions

Engineering rules and architecture guidelines for contributing to Nascope.

To keep documentation focused and modular, conventions are split into dedicated topic documents below. **Read the specific document relevant to your current task.**

---

## Conventions Index

| Topic | Document | When to Read |
| :--- | :--- | :--- |
| **Project Layout** | [`docs/conventions/project-layout.md`](./conventions/project-layout.md) | Adding new files, structuring packages, understanding directory responsibilities. |
| **Design System & Tokens** | [`docs/conventions/design-system.md`](./conventions/design-system.md) | Styling UI, choosing colors, typography (Inter vs JetBrains Mono), surface elevation layers. |
| **Composable API & Performance** | [`docs/conventions/compose-api.md`](./conventions/compose-api.md) | Creating reusable composables, parameter ordering, previews, animation labels, draw phase rules. |
| **Kotlin Style** | [`docs/conventions/kotlin-style.md`](./conventions/kotlin-style.md) | Formatting, trailing commas, imports, naming, and immutability annotations. |
| **Dashboard & Brick Architecture** | [`docs/conventions/dashboard-architecture.md`](./conventions/dashboard-architecture.md) | Building dashboard grids, implementing `DashboardBrick`, `BrickSpan`, responsive column scaling. |
| **ViewModel & MVI Architecture** | [`docs/conventions/viewmodel-mvi.md`](./conventions/viewmodel-mvi.md) | Implementing ViewModels, `ViewState`, `ViewAction`, state holders, and Koin injection. |
| **Widget & Screen Composition** | [`docs/conventions/widget-composition.md`](./conventions/widget-composition.md) | Connecting ViewModels to UI, stateful vs stateless composables, `WidgetHeader`, cards. |
| **Unit Testing** | [`docs/conventions/unit-testing.md`](./conventions/unit-testing.md) | Writing JVM unit tests with `TestCoroutineRule`, Turbine, Mockk, and companion test fixtures. |
| **Task Workflow** | [`docs/conventions/task-workflow.md`](./conventions/task-workflow.md) | Implementing tasks from `PLAN.md`, verification commands, showcase updates, commit rules. |

---

## Golden Rules Summary

1. **Tokens only**: Never hardcode hex colors, dp, or sp. Use `AppTheme.colors.*`, `typography.*`, `spacing.*`, `shapes.*`.
2. **Dashboard as container**: `DashboardScreen` only arranges bricks via `LazyVerticalGrid` and delegates rendering to `brick.Content(modifier)`. It never previews or manages widget internals.
3. **MVI Unidirectional Data Flow**: ViewModels expose immutable `StateFlow<ViewState>` and process user intents via `onViewAction(ViewAction)`.
4. **Stateful vs Stateless UI**: Separate composables into a stateful entrypoint (Koin + lifecycle) and a pure stateless composable (`viewState` + `onViewAction`).
5. **Unit test standard**: Place tests in `shared/src/jvmTest/kotlin/`. Use `@get:Rule val coroutineRule = TestCoroutineRule()`, test flows with Turbine, and put test data in a `companion object` at the end of the file.
6. **No git commit**: Never run `git commit`. Leave all changes in the working tree for review.
