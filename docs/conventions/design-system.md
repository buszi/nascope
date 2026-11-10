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
