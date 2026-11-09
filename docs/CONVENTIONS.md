# Code Conventions

Rules for contributing to Nascope (people and AI agents alike). Read this before you write code. For the design token reference, see the [README](../README.md#obsidian-telemetry-design-system). For the task roadmap, see [PLAN.md](../PLAN.md).

---

## 1. Project Layout

| Location | Contents |
| :--- | :--- |
| `shared/src/commonMain/kotlin/io/buszi/nascope/` | All shared UI and business logic. |
| `.../ui/common/theme/` | Design system tokens (`AppTheme`, colors, typography, shapes, spacing). |
| `.../ui/common/components/<group>/` | Reusable UI primitives grouped by kind: `containers`, `badges`, `metrics`, `gauges`, `inputs`, … |
| `.../ui/dashboard/widgets/` | Dashboard "bricks" built from the common components. |
| `shared/src/androidMain/`, `shared/src/jvmMain/` | Platform-specific code only, via `expect`/`actual` or interfaces. |
| `resources/src/commonMain/composeResources/` | Shared drawables, strings, fonts. Access them via `io.buszi.nascope.resources.Res`. |

- **Put one public composable in each file.** Name the file after the composable (e.g. `StatusChip.kt`). Overloads of the same composable may share a file (e.g. both `TelemetryCard` variants).
- Supporting types that belong to a component group, such as enums or color mappings (e.g. `StatusLevel`), go in their own file in the same package.
- If code is shared within a group but isn't part of the public API, mark it `internal` and give it its own file (e.g. `ChipSurface.kt`).

---

## 2. Design Tokens: Never Hardcode

- Always take colors, typography, shapes, spacing, and dimensions from `AppTheme`:
  `AppTheme.colors.*`, `AppTheme.typography.*`, `AppTheme.shapes.*`, `AppTheme.spacing.*`, `AppTheme.dimensions.*`.
- **Never use raw hex `Color(0x…)`, `N.dp`, or `N.sp` in components.** If you need an in-between value, combine tokens (`spaceXs + space2xs`). If a value keeps coming back, add a new token to the theme instead of hardcoding it.
- Exception: unitless visual constants that are internal to one component (animation durations, scale factors, alpha multipliers). Declare them as `private const val` at the top of the file.
- Map semantic state to colors in exactly one place. Example: `StatusLevel.color` / `.haloColor` / `.badgeBackgroundColor` / `.badgeBorderColor`. Don't pick status colors at the call site.

### Surface hierarchy (1px borders, no drop shadows)

| Layer | Fill | Border | Component |
| :--- | :--- | :--- | :--- |
| 0: Canvas | `canvasBackground` | none | screen root |
| 1: Cards / panels | `subSurface` | `baseBorder` | `TelemetryCard` |
| 2: Overlays / dialogs | `elevatedOverlay` | `elevatedBorder` | `ElevatedPanel` |

### Typography

- **Inter** (`headline*`, `body*`, `labelUi`): navigation, structure, prose.
- **JetBrains Mono** (`metricDisplay`, `metricMd`, `codeBase`, `labelCode`): every number, metric, IP/CIDR, address, timestamp, identifier, and badge label. These styles have tabular numerals (`tnum`) turned on.

---

## 3. Composable API Shape

Parameter order follows the [Compose API guidelines](https://developer.android.com/develop/ui/compose/api-guidelines):

1. Required parameters (`text`, `status`, …)
2. `modifier: Modifier = Modifier`
3. Optional behavior flags (`showDot`, `pulsing`, …)
4. Optional style overrides that default to theme tokens (`backgroundColor = AppTheme.colors.subSurface`, `shape = AppTheme.shapes.base`, …)
5. Trailing `content` / slot lambda, if any

```kotlin
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    pulsing: Boolean = false,
    backgroundColor: Color = AppTheme.colors.subSurface,
    borderColor: Color = AppTheme.colors.baseBorder,
    textColor: Color = AppTheme.colors.textSecondary,
)
```

- Apply `modifier` first, on the root layout node only.
- Use a nullable slot (`(@Composable RowScope.() -> Unit)? = null`) or nullable value (`Color? = null`) for optional visual pieces. Avoid adding a separate boolean next to them.
- Expose animation behavior as a plain parameter (e.g. `pulsing: Boolean`) so the caller decides when to animate.
- Only `clip()` when the content really needs it. `background(color, shape)` and `border(width, color, shape)` already respect the shape, and clipping cuts off halos and ripples.

---

## 4. Documentation & Previews

- Every public composable gets a **KDoc** that covers its purpose, which layer/tokens it uses, and any parameter that isn't obvious (`@param`).
- Every component file contains a **`private` `@Preview`** named `<Component>Preview`:
  - Wrap it in `AppTheme { … }`.
  - Show the meaningful variants (each state, with and without optional parts, animated) next to each other.
  - Use realistic telemetry sample data (`eth0`, `/volume1`, `842.6 MB/s`), not "Lorem ipsum".
- Comments explain *why*, not *what*.

---

## 5. Performance & Animation

- Give every animation API a `label` (`animateColorAsState(…, label = "StatusChipAccent")`) so it can be inspected in tooling.
- Read rapidly changing animation state in the **draw phase** (`drawBehind`, `graphicsLayer { }`) instead of composition, so the composable doesn't recompose every frame.
- Use `animate*AsState` for color/size transitions between states, so live telemetry updates don't snap.

---

## 6. Kotlin Style

- 4-space indentation and **trailing commas** in multi-line parameter/argument lists.
- Named arguments for anything past the first obvious parameter, and always for style overrides.
- Explicit imports only. No wildcard imports, and no fully-qualified names inline (`androidx.compose.foundation.BorderStroke(...)`).
- `private const val` constants in `PascalCase` (e.g. `PulseDurationMillis`).
- Mark immutable theme/state data classes with `@Immutable`.

---

## 7. Task Workflow

Each task from [PLAN.md](../PLAN.md) is self-contained:

1. Implement the deliverables following the conventions above.
2. **Use the new components in the showcase** (`App.kt`) and remove any ad-hoc private versions they replace.
3. **Verify the build**:
   ```bash
   ./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain   # fast check
   ./gradlew assemble                                              # full build
   ./gradlew :shared:jvmTest                                       # when tests exist
   ```
4. Tick the task's checkbox in `PLAN.md`.
5. Update the README or this document if the task adds a new convention, module, or token.
6. **Do NOT run `git commit`.** Leave all changes uncommitted in the working tree for the user to review and commit.

---

## 8. Known Legacy Deviations

- `App.kt` → buttons use an inline fully-qualified `BorderStroke` and raw Material buttons. Replace these with `PrimaryButton` / `SecondaryButton` (TASK-1.4).
