# Project Layout Conventions

Structure and organization for code in the Nascope codebase.

---

## Modules

See [`module-architecture.md`](./module-architecture.md) for responsibilities and dependency direction.

| Module | Contents |
| :--- | :--- |
| `domain-model/` | Shared domain models (`io.buszi.nascope.domain.model`), `domain/model/connection/` and `domain/model/telemetry/`. Data only, no dependencies. |
| `storage/` | Local persistence (`io.buszi.nascope.storage`): public `ServerProfileStore`, `AppPreferencesStore`, `SecretStore`; internal DataStore, Keystore (`androidMain`) and keyring (`jvmMain`) implementations. |
| `network/` | Remote data access (`io.buszi.nascope.network`): public `GlancesConnectionClient` and `GlancesTelemetryClient`; internal Ktor/OkHttp implementation, URL building and error mapping. |
| `domain/` | Business logic (`io.buszi.nascope.domain`): use cases in `connection/` (public `fun interface` with nested result models); `internal` `@Factory` implementations in `connection/impl/`, one file each, taking the injected `CoroutineDispatcher`; internal telemetry sources in `telemetry/`. |
| `test-utils/` | Shared JVM test helpers (`io.buszi.nascope.test`): `TestCoroutineRule`, Turbine extensions. Added as a `jvmTest` dependency only. |
| `resources/` | Compose resources (strings, drawables, fonts). |
| `shared/` | UI, ViewModels, navigation, design system, Koin application root. |

---

## Source Directory Layout

| Location | Contents |
| :--- | :--- |
| `shared/src/commonMain/kotlin/io/buszi/nascope/` | All shared UI, state holders, and business logic. |
| `.../ui/common/theme/` | Design system tokens (`AppTheme`, colors, typography, shapes, spacing, dimensions). |
| `.../ui/common/components/<group>/` | Reusable UI primitives grouped by category: `containers`, `badges`, `metrics`, `gauges`, `inputs`, ... |
| `.../ui/common/util/` | Shared UI utilities and layout helpers (e.g., `WindowWidthClass`, `LocalWindowWidthClass`). |
| `.../ui/navigation/` | Adaptive Navigation 3 scaffold (`NascopeAppScaffold`), routes (`NavRoute`), backstack state (`NascopeNavigator`), and multi-pane scene strategy (`DashboardSplitSceneStrategy`). |
| `.../ui/navigation/components/` | Adaptive navigation chrome (`NascopeSidebarRail`, `NascopeBottomBar`, `NascopeTopBar` with `NascopeTopBarViewModel` for the live node name and connection status, `NodeSwitcher` quick-switch menu, `NavigationGlyph`). |
| `.../ui/root/` | Root gate (`AppRoot`, `AppRootViewModel`): loading canvas, setup flow or navigation scaffold, decided from the session. |
| `.../ui/setup/` | Setup flow shown before any server exists, outside the scaffold: `SetupRoute` keys, `SetupFlow` (own Navigation 3 back stack), `WelcomeScreen` + `WelcomeViewModel`. |
| `.../ui/nodes/` | Node management (`NodesScreen`, `NodesViewModel`, `NodeCard`): stored servers with set active, edit, delete with confirmation, add, and the demo-mode notice. |
| `.../ui/connection/` | Connection editor (`ConnectionEditorScreen`, `ConnectionEditorViewModel`, `ConnectionCheckRow`): one form for adding a server (setup flow) and editing one by profile id. |
| `.../ui/dashboard/` | Dashboard container screen and orchestration ViewModel. |
| `.../ui/dashboard/framework/` | Dashboard brick contracts (`DashboardBrick`, `BrickSpan`, `BrickRegistry`). |
| `.../ui/dashboard/widgets/<name>/` | Dashboard "bricks" built from common components (e.g., `system/`, `storage/`). |
| `.../ui/details/`, `.../ui/nodes/`, `.../ui/settings/` | Destination screens for brick details/inspector, cluster node management, and application settings. |
| `shared/src/androidMain/`, `shared/src/jvmMain/` | Platform-specific code only, via `expect`/`actual` or platform-specific interfaces. |
| `<module>/src/jvmTest/kotlin/` | Unit tests of that module (`shared`: ViewModels, state holders, utilities; `domain`: use cases; ...). |
| `resources/src/commonMain/composeResources/` | Shared drawables, strings, fonts. Access via `io.buszi.nascope.resources.Res`. |

---

## File & Class Organization Rules

- **One public composable per file**: Name the file strictly after the composable (e.g. `StatusChip.kt`, `TelemetryCard.kt`). Overloads of the same composable may share a file.
- **Group supporting types**: Supporting types belonging to a component category (such as enums, state models, or color mappers like `StatusLevel`) go in their own dedicated file within the same package.
- **Internal visibility**: If a helper or surface is shared within a package group but is not part of the public API, mark it `internal` and place it in its own file (e.g. `ChipSurface.kt`).
- **Feature packaging**: Feature widgets/bricks are co-located in their own directory under `ui/dashboard/widgets/<feature>/` containing the brick definition, ViewModel, stateful widget, stateless content composable, and preview.
