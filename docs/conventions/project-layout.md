# Project Layout Conventions

Structure and organization for code in the Nascope codebase.

---

## Source Directory Layout

| Location | Contents |
| :--- | :--- |
| `shared/src/commonMain/kotlin/io/buszi/nascope/` | All shared UI, state holders, and business logic. |
| `.../ui/common/theme/` | Design system tokens (`AppTheme`, colors, typography, shapes, spacing, dimensions). |
| `.../ui/common/components/<group>/` | Reusable UI primitives grouped by category: `containers`, `badges`, `metrics`, `gauges`, `inputs`, ... |
| `.../ui/common/util/` | Shared UI utilities and layout helpers (e.g., `WindowWidthClass`). |
| `.../ui/dashboard/` | Dashboard container screen and orchestration ViewModel. |
| `.../ui/dashboard/framework/` | Dashboard brick contracts (`DashboardBrick`, `BrickSpan`, `BrickRegistry`). |
| `.../ui/dashboard/widgets/<name>/` | Dashboard "bricks" built from common components (e.g., `system/`, `storage/`). |
| `.../data/` | Data layer, repositories, telemetry contracts, and state holders (`telemetry/`). |
| `shared/src/androidMain/`, `shared/src/jvmMain/` | Platform-specific code only, via `expect`/`actual` or platform-specific interfaces. |
| `shared/src/jvmTest/kotlin/` | All unit tests (ViewModels, state holders, utilities). |
| `resources/src/commonMain/composeResources/` | Shared drawables, strings, fonts. Access via `io.buszi.nascope.resources.Res`. |

---

## File & Class Organization Rules

- **One public composable per file**: Name the file strictly after the composable (e.g. `StatusChip.kt`, `TelemetryCard.kt`). Overloads of the same composable may share a file.
- **Group supporting types**: Supporting types belonging to a component category (such as enums, state models, or color mappers like `StatusLevel`) go in their own dedicated file within the same package.
- **Internal visibility**: If a helper or surface is shared within a package group but is not part of the public API, mark it `internal` and place it in its own file (e.g. `ChipSurface.kt`).
- **Feature packaging**: Feature widgets/bricks are co-located in their own directory under `ui/dashboard/widgets/<feature>/` containing the brick definition, ViewModel, stateful widget, stateless content composable, and preview.
