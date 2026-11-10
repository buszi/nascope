# Composable API & Performance Conventions

Rules for writing composable APIs, handling previews, animations, and performance.

---

## 1. Composable Parameter Order

Follow the [Android Compose API Guidelines](https://developer.android.com/develop/ui/compose/api-guidelines):

1. **Required parameters** (`text`, `status`, `viewState`, …)
2. `modifier: Modifier = Modifier`
3. **Optional behavior flags** (`showDot`, `pulsing`, …)
4. **Optional style overrides** defaulting to theme tokens:
   `backgroundColor: Color = AppTheme.colors.subSurface`, `shape: Shape = AppTheme.shapes.base`
5. **Trailing content slot lambda**, if any (`content: @Composable () -> Unit`)

### Example:
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

---

## 2. Slots & Styling Guidelines

- **Modifier application**: Apply `modifier` parameter first on the root layout node only.
- **Optional visual slots**: Use nullable slots (`(@Composable RowScope.() -> Unit)? = null`) or nullable values (`Color? = null`) for optional visual pieces. Avoid adding separate boolean flags alongside them.
- **Animation flags**: Expose animation states as plain parameters (e.g., `pulsing: Boolean = false`) so the caller or parent state controls animation triggers.
- **Clipping**: Only apply `Modifier.clip(...)` when child contents genuinely require bounds clipping. `background(color, shape)` and `border(width, color, shape)` automatically respect shapes; gratuitous clipping cuts off glowing halos, borders, and touch ripples.

---

## 3. Documentation & Previews

- **KDocs**: Every public composable requires a KDoc describing its purpose, surface layer/tokens used, and non-obvious parameters (`@param`).
- **Component Previews**:
  - Every component file must include a `private` `@Preview` named `<Component>Preview`.
  - Always wrap the preview in `AppTheme { ... }`.
  - Preview meaningful variants (loading, active, warning, empty) side-by-side.
  - Use realistic telemetry data (`alpha-node-01`, `eth0`, `842.6 MB/s`), never filler "Lorem ipsum".

---

## 4. Performance & Animation

- **Animation Inspection**: Provide an explicit `label` argument to all animation calls:
  ```kotlin
  animateColorAsState(..., label = "StatusChipAccent")
  ```
- **Draw Phase Optimization**: Read rapidly updating animation or gesture states during the **draw phase** (`Modifier.drawBehind`, `Modifier.graphicsLayer { ... }`) rather than in composition, preventing unnecessary recompositions every frame.
- **Smooth State Transitions**: Use `animate*AsState` for color and dimension transitions between operational states so live telemetry updates transition smoothly without jarring visual snaps.
