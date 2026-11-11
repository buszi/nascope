---
sessionId: session-261111-093656-19t7
---

# Requirements

### Overview & Goals
Implement TASK-3.2: dashboard bricks use modular height units (1U) on multi-column grids so rows are level with no empty gaps. The convention is set for all 8 existing bricks.

### Scope
**In scope**
- `AppDimensions.brickHeightUnit = 250.dp` and a `BrickHeight` contract (1U, 2U, ...), declared per brick.
- Fixed heights on Medium/Expanded grids; Compact (mobile) keeps natural wrap-content height.
- Compact, glanceable content for all bricks so they fit their declared height (CPU, Containers, Process List need the most work).
- Update PLAN.md (task 3.2 checked, deliverables adjusted).

**Out of scope**
- Drill-down click callback `onOpenDetails`, chevrons and detail views (separate task, 3.4).
- Navigation and the adaptive grid column rework (3.3, 3.5).

### Functional Requirements
- A brick of height N units is `N*250dp + (N-1)*gutter` tall, so a 2U brick aligns with two stacked 1U bricks.
- Content never overflows or clips: lists show top-N rows (Containers top 3, Process List top 4-5, CPU 4-column core grid) and summarise the rest, for example `+5 more`.
- Mobile / Compact width: no fixed height.

# Technical Design

### Current Implementation
- `DashboardScreen` uses `LazyVerticalGrid` with 2/3/4 fixed columns (by `toWindowWidthClass()`); bricks are rendered with `brick.Content(Modifier.fillMaxWidth())`, so rows take the tallest item's height.
- `DashboardBrick` (framework/DashboardBrick.kt) has only `id`, `span: BrickSpan`, `Content(modifier)`. `BrickRegistry` lists 8 bricks.
- `DashboardWidget` is the shared card shell (Column with header + content); it sizes to content.
- `CpuMatrixWidget` switches Wide/Compact layouts by container width and renders all cores in rows.
- `AppDimensions` lives in `ui/common/theme/Spacing.kt`.

### Key Decisions
- Height is declared per brick via a `BrickHeight` value (user-selected), not forced globally.
- The grid owns height enforcement (one place), via `Modifier.height` on multi-column layouts. Widgets get their height via `DashboardWidget` filling its bounds, and decide how much content to show based on the available height.
- Compact windows (2 columns on phones) use the existing wrap-content behavior as the plan specified for single-column mobile; to be precise, enforcement is applied only for Medium/Expanded.

### Data Models / Contracts
```kotlin
// DashboardBrick.kt
@Immutable
sealed interface BrickHeight {
    data class Units(val units: Int = 1) : BrickHeight
    // resolve to Dp: units * unit + (units - 1) * gutter
}
interface DashboardBrick { val height: BrickHeight /* default Units(1) */ ; ... }

// Spacing.kt AppDimensions
val brickHeightUnit: Dp = 250.dp
```
`BrickHeight.resolve(unit: Dp, gutter: Dp): Dp` is a pure function so it can be unit-tested in `BrickSpanTest`-style jvmTest.

### Proposed Changes
1. Add `brickHeightUnit` to `AppDimensions`; add `BrickHeight` and `height` to `DashboardBrick`.
2. `DashboardScreen`: compute `fixedHeights = windowWidthClass != Compact`; apply `Modifier.fillMaxWidth().height(brick.height.resolve(...))` else `fillMaxWidth()`.
3. `DashboardWidget`: add optional `fillHeight` behavior (content area uses `weight(1f)` + clipping when the incoming modifier has a bounded height) so card chrome stretches to the slot.
4. Per-brick content: CPU (4-col core grid, 2U or compact fit for 16 cores), Containers (top 3 + overflow count), Process List (top 4-5 rows + overflow), and verify System Overview, Memory, Storage, Network, Sensors fit 1U (trim rows or use overflow summary).
5. Choose each brick's height in its `*Brick` object (e.g. CPU 1U beside System Overview, or 2U if it doesn't fit) and update column-span/height combos so rows fill without gaps.
6. Update PLAN.md TASK-3.2 text.

### Risks
- Different phone/tablet widths can make text wrap and exceed 250dp; mitigate with row-count limits and clipping, and check previews at each width class.
- Spans plus 2U heights in `LazyVerticalGrid` can leave holes since it has no row-span; prefer 1U for all bricks unless a 2U is shown to pack well.

# Testing

### Validation Approach
- jvmTest unit tests for `BrickHeight.resolve` (1U, 2U, 3U with gutter) and that all registered bricks declare a valid height.
- Update existing `DashboardViewModelTest` / `BrickSpanTest` if constructor/contract changes.
- Run `./gradlew :shared:jvmTest` and compile Android and Desktop targets.

### Edge Cases
- Empty or very long lists (0, 3, 50 containers/processes).
- Loading/error/offline widget states keep the same fixed height.
- 16-core and 2-core CPUs.

# Delivery Steps

### ✓ Step 1: Add height-unit contract and enforce it in the grid
Bricks declare a BrickHeight and DashboardScreen applies equal fixed heights on Medium/Expanded.

- Add `brickHeightUnit = 250.dp` to `AppDimensions` in `Spacing.kt`.
- Add `BrickHeight` (with pure `resolve(unit, gutter)`) and `height` (default 1U) to `DashboardBrick`.
- Apply `Modifier.height(...)` in `DashboardScreen` only for non-Compact width classes.
- Make `DashboardWidget` fill and clip to a bounded height.
- Add jvmTest for `BrickHeight.resolve`.

### ✓ Step 2: Make CPU, Containers and Process List glanceable
The three tallest bricks fit within their declared height with top-N previews.

- `CpuMatrixWidget`: 4-column core grid so 16 cores fit.
- `ContainersWidget`: top 3 rows with an overflow count.
- `ProcessListWidget`: top 4-5 rows with an overflow count.
- Update previews and ViewModel-facing tests if row limits move into state.

### ✓ Step 3: Fit remaining bricks, tune heights and update PLAN.md
All 8 bricks sit level on the grid with no gaps.

- Check System Overview, Memory, Storage, Network and Sensors against 1U and trim content where needed.
- Set each brick's `height` and span so the rows pack without gaps.
- Mark TASK-3.2 done in PLAN.md and record the height-unit convention and the deferred drill-down callback.
- Run `./gradlew :shared:jvmTest` plus Desktop and Android compile.