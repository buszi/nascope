# Design System & Token Conventions

Nascope follows the **Obsidian Telemetry** design system. All visual attributes are derived strictly from theme tokens.

---

## 1. Never Hardcode Visual Values

- Always retrieve colors, typography, shapes, spacing, and dimensions from `AppTheme`:
  - `AppTheme.colors.*`
  - `AppTheme.typography.*`
  - `AppTheme.shapes.*`
  - `AppTheme.spacing.*`
  - `AppTheme.dimensions.*`
- **Never use raw hex colors `Color(0x...)`, `N.dp`, or `N.sp` directly in components.**
- If you need an in-between spacing value, combine existing tokens (e.g. `AppTheme.spacing.spaceXs + AppTheme.spacing.space2xs`).
- If a specific dimension or color value recurs across multiple components, propose adding a semantic token to `AppTheme` rather than hardcoding it.
- **Exception**: Unitless visual constants that are strictly internal to one component (animation durations in milliseconds, scale ratios, alpha multipliers). Declare these as `private const val` at the top of the component file.

---

## 2. Surface Hierarchy (1px Borders, No Drop Shadows)

Nascope uses subtle 1px borders and layered fills to communicate elevation, never drop shadows:

| Layer | Fill Token | Border Token | Primary Component | Typical Usage |
| :--- | :--- | :--- | :--- | :--- |
| **0: Canvas** | `canvasBackground` | None | Screen root | `DashboardScreen`, background canvas |
| **1: Cards / Panels** | `subSurface` | `baseBorder` | `TelemetryCard` | Dashboard widget bricks, containers |
| **2: Overlays / Dialogs** | `elevatedOverlay` | `elevatedBorder` | `ElevatedPanel` | Dropdown menus, modals, dialogs |

---

## 3. Typography Rules

The typography system uses two strict typefaces with dedicated roles:

- **Inter** (`headline*`, `body*`, `labelUi`):
  - Used for navigation, layout headings, structure, and prose.
- **JetBrains Mono** (`metricDisplay`, `metricMd`, `codeBase`, `labelCode`):
  - Used for **every** numeric value, metric read-out, IP address, CIDR, hash, timestamp, identifier, and badge label.
  - Tabular numerals (`tnum`) are enabled by default so numerical values align vertically without jitter.

---

## 4. Semantic Color Mapping

- Semantic state must be mapped to color tokens in **exactly one place** (e.g., in `StatusLevel.color`, `StatusLevel.badgeBackgroundColor`, `StatusLevel.badgeBorderColor`).
- Never choose status colors ad-hoc at the call site.

---

## 5. Form Primitives (`ui/common/components/inputs/`)

Building blocks for forms such as the connection editor. All of them use tokens only, take an optional `interactionSource`, and are focusable with keyboard on desktop.

| Component | Use it for | Notes |
| :--- | :--- | :--- |
| `FormField` | Label above any input, optional helper and error text below. | An error replaces the helper (`AppTheme.colors.critical`) and is announced as a polite live region. Wrap `TelemetryTextField`, `PasswordTextField` or `SegmentedSelector` in it. |
| `TelemetrySwitch` | On/off setting, optionally with a label. | Same surface language as `SelectableChip` / `TelemetryTextField`: `inputBackground` track, Electric Violet when on, focus ring. The whole row is the touch target. |
| `PasswordTextField` | Secrets. | `TelemetryTextField` with masking and a trailing show/hide action. Labels are passed in (`showLabel`, `hideLabel`) so strings stay in `strings.xml`. |
| `SegmentedSelector` | 2-3 exclusive options (e.g. `HTTP` / `HTTPS`). | A new component: `SelectableChip`s are separate pills with gaps and do not read as one control. Use `SelectableChip` for filter/sort chips, `SegmentedSelector` for a single choice inside a form. |

- **Touch targets:** on `WindowWidthClass.Compact` interactive form components grow to `AppDimensions.minTouchTarget` (48dp) through `Modifier.minimumTouchTarget()` (`ui/common/util`). Apply it before any fixed `height(...)`. `TelemetryTextField` itself stays 32dp; add `Modifier.minimumTouchTarget()` when using it directly in a form on Compact (`PasswordTextField` does it already).
- **Focus previews:** previews use `rememberFocusedInteractionSource()` (internal to the package) to show the focused state.

### Focused single-column screens

`CenteredPanel` (`ui/common/components/containers/`) lays out setup-style screens: on Compact the content fills the width inside the standard margin, on Medium and Expanded it is a centred column limited to `AppDimensions.setupPanelMaxWidth` (480dp). It centres vertically and scrolls when the content does not fit. Use it for the welcome screen and the connection editor instead of building new layouts.

