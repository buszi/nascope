# Nascope

**Nascope** is a Kotlin Multiplatform telemetry and monitoring application engineered for systems engineers, homelab operators, and infrastructure architects. It targets **Android** and **Desktop (JVM)** with a unified UI built with **Compose Multiplatform** and **Material 3**.

---

## Architecture & Module Structure

The project uses Gradle type-safe project accessors and custom convention plugins under `build-logic`:

```
nascope/
├── build-logic/convention/       # Gradle convention plugins
│   └── plugins:
│       ├── plugins.kmp.library          # Multiplatform library setup
│       ├── plugins.kmp.compose          # Compose Multiplatform dependencies & compiler
│       ├── plugins.android.application  # Android application conventions
│       └── plugins.desktop.application  # Desktop JVM application conventions
├── domain-model/                 # Shared domain models (plain Kotlin, no dependencies)
│   └── src/commonMain/kotlin/io/buszi/nascope/domain/model/
├── domain/                       # Use cases (connection management)
│   └── src/commonMain/kotlin/io/buszi/nascope/domain/connection/
├── network/                      # Glances HTTP access (Ktor + OkHttp), connection client
│   └── src/{commonMain,androidMain,jvmMain}/kotlin/io/buszi/nascope/network/
├── storage/                      # Profile & preference stores (DataStore), secret store (Keystore / OS keyring)
│   └── src/{commonMain,androidMain,jvmMain}/kotlin/io/buszi/nascope/storage/
├── test-utils/                   # Shared JVM test helpers (TestCoroutineRule, Turbine extensions)
├── resources/                    # Common Compose Multiplatform assets & resources
│   └── src/commonMain/composeResources/
├── shared/                       # Core shared business logic & UI components
│   ├── src/commonMain/kotlin/
│   │   └── io/buszi/nascope/
│   │       ├── di/               # Koin root module + initNascope() (Koin + Napier startup)
│   │       ├── App.kt            # Root composable & Navigation 3 scaffold entrypoint
│   │       ├── ui/common/theme/  # Obsidian Telemetry Design System
│   │       ├── ui/common/components/  # Reusable UI primitives
│   │       ├── ui/root/          # Root gate: loading / setup flow / main scaffold
│   │       ├── ui/connection/    # Connection editor: add / edit a server with a live test
│   │       ├── ui/setup/         # Welcome & connection setup flow (outside the scaffold)
│   │       ├── ui/navigation/    # Adaptive Navigation 3 scaffold, routes & multi-pane scene strategy
│   │       ├── ui/dashboard/     # Modular telemetry dashboard & brick widgets
│   │       ├── ui/details/       # Brick detail inspector / drill-down screen
│   │       ├── ui/nodes/         # Cluster node management screen
│   │       └── ui/settings/      # Application settings screen
│   ├── src/androidMain/          # Android-specific implementations
│   └── src/jvmMain/              # JVM-specific implementations
├── androidApp/                   # Android entry point (NascopeApplication, MainActivity)
├── desktopApp/                   # Desktop JVM entry point (main.kt)
└── docs/                         # Developer documentation (CONVENTIONS.md)
```

---

## Obsidian Telemetry Design System

The application theme is located in [`io.buszi.nascope.ui.common.theme`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme) and is modeled after the **Obsidian Telemetry** design specification (OLED-optimized, low-light, high-contrast, technical instrument aesthetic).

### Theme Accessor
Theme tokens are exposed via `AppTheme` within any `@Composable`:

```kotlin
AppTheme {
    // Access colors, typography, shapes, spacing, and dimensions
    val bg = AppTheme.colors.canvasBackground
    val metricStyle = AppTheme.typography.metricDisplay
    val cardShape = AppTheme.shapes.lg
    val pad = AppTheme.spacing.spaceLg
}
```

### Theme Components

| File | Responsibilities & Tokens |
| :--- | :--- |
| **[`AppTheme.kt`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme/AppTheme.kt)** | Entry composable `AppTheme(...)` providing `CompositionLocalProvider` for all token groups; `object AppTheme` for static, ergonomic token access (`colors`, `colorScheme`, `typography`, `materialTypography`, `shapes`, `materialShapes`, `spacing`, `dimensions`, `platform`, `windowWidthClass`). |
| **[`Colors.kt`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme/Colors.kt)** | • **`DarkColorScheme`**: Full Material 3 dark color scheme mapping.<br>• **`AppColors`**:<br>&nbsp;&nbsp;- *Base / Surfaces*: `canvasBackground` (`#0B0F17`), `subSurface` (`#12141A`), `elevatedOverlay` (`#181C24`), `activeHoverRowFill` (`#1E232F40`), `inputBackground` (`#0E121A`).<br>&nbsp;&nbsp;- *Borders*: `baseBorder` (`#1E232F`), `subtleDivider` (`#171B26`), `focusHighlight` (`#384259`), `elevatedBorder` (`#282E3E`).<br>&nbsp;&nbsp;- *Telemetry*: `operational` (`#10B981`) + halo, `degraded` (`#F59E0B`) + halo, `critical` (`#F43F5E`) + halo, `telemetryA` (`#8B5CF6` core pipelines), `telemetryB` (`#06B6D4` network I/O).<br>&nbsp;&nbsp;- *Text Stack*: `textPrimary` (`#F1F5F9`), `textSecondary` (`#94A3B8`), `textMuted` (`#64748B`), `textDisabled` (`#334155`).<br>&nbsp;&nbsp;- *Badges & Buttons*: `healthyBadgeBackground` / `border`, `warningBadgeBackground` / `border`, `criticalBadgeBackground` / `border`, `buttonPrimary`, `buttonPrimaryHover` (`#7C3AED`). |
| **[`Typography.kt`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme/Typography.kt)** | • **Dual-Type Strategy**:<br>&nbsp;&nbsp;- **Inter** (`interFontFamily`): `headlineXl` (36sp), `headlineXlMobile` (28sp), `headlineLg` (24sp), `headlineSm` (18sp), `bodyLg` (15sp), `bodyMd` (14sp), `bodySm` (12sp), `labelUi` (11sp with positive tracking `0.04em`).<br>&nbsp;&nbsp;- **JetBrains Mono** (`monoFontFamily`): `metricDisplay` (32sp), `metricMd` (20sp), `codeBase` (13sp), `labelCode` (11sp with `0.02em` tracking). All telemetry styles use **tabular numerals** (`fontFeatureSettings = "tnum"`).<br>• Fully mapped to Material 3 `Typography`. |
| **[`Shapes.kt`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme/Shapes.kt)** | Tight technical radiuses: `sm` (2dp), `base` (4dp), `md` (6dp), `lg` (8dp), `xl` (12dp), `full` (CircleShape / 9999px for status dots and badges). Also configures Material 3 `Shapes`. |
| **[`Spacing.kt`](./shared/src/commonMain/kotlin/io/buszi/nascope/ui/common/theme/Spacing.kt)** | • **`AppSpacing`**: Micro & structural cadence (`space2xs` = 2dp, `spaceXs` = 4dp, `spaceSm` = 8dp, `spaceMd` = 12dp, `spaceLg` = 16dp, `spaceXl` = 24dp, `space2xl` = 32dp), margins, and gutters.<br>• **`AppDimensions`**: Hairline border (1dp), focus ring (2dp), status dot (6dp), button heights (32dp dense / 36dp regular), input height (32dp), switch track (32 x 18dp) and thumb (12dp), minimum touch target (48dp, applied on Compact windows), setup panel max width (480dp), table header (28dp), sidebar desktop (240dp), sidebar tablet (56dp), top bar height (52dp), bottom bar height (60dp), detail inspector width (360dp), log drawer (320dp), command palette width (580dp). |

---

## Application Architecture & Concepts

### Glances Telemetry Backend
Nascope connects to hosts running the [Glances](https://github.com/nicolargo/glances) monitoring service via its REST API (default port `61208`).
- **Connection Lifecycle**: First-launch onboarding captures base URL, port, protocol, and optional authentication secret. A pre-flight handshake validates DNS resolution, HTTP status, and Glances API status before entering the live dashboard.
- **Telemetry Collection**: Data is collected through periodic interval polling (e.g., 2-3s active foreground, throttled when minimized or backgrounded) rather than persistent WebSockets, matching Glances REST capabilities.
- **Offline & Mock-First Strategy**: The data layer is decoupled behind a repository interface. A synthetic data generator simulates live fluctuations and subsystem alerts, enabling rapid UI development and offline testing without requiring a live NAS connected.

### Adaptivity & Screen Form Factor Strategy
Nascope is designed as a premier showcase of Compose Multiplatform adaptivity, presenting an optimal layout across all screen factors without platform-branching UI logic:
- **Mobile (< 768dp)**: 4-column adaptive layout, 16dp margins, 12dp gutters. Sparkline cards stack into single columns; complex tables transform into swipeable cards with sticky left-most node identifiers.
- **Tablet (768dp - 1279dp)**: 8-column layout, 24dp margins, 16dp gutters. Sidebars collapse to a 56dp icon rail; metric panels form 2x2 grids.
- **Desktop (>= 1280dp)**: 12-column fluid grid, 32dp margins, 20dp gutters. Primary canvas features a split workbench layout: 240dp fixed utility sidebar, scalable telemetry workspace, and an optional 320dp live log/event drawer.

### Composable "Bricks" Dashboard
The dashboard is conceived as a modular surface built from independent, reusable "bricks" (widgets):
- **Encapsulated Scope**: Each brick (System, CPU Matrix, Memory & Swap, Storage Pools, Network I/O, Sensors/Thermals, Containers, Process List) owns its rendering, loading state, visual thresholds, and error fallbacks.
- **Fluid Grid Spans**: Bricks define minimum and preferred column spans to naturally wrap and reflow across window breakpoints.
- **User Customization**: Users will be able to customize their telemetry cockpit by toggling visibility, reordering widgets, or selecting preset layouts (e.g., Storage Focus, Compute Focus, Minimalist).

### Platform Extensions
Shared core features are augmented with desktop and mobile platform capabilities:
- **Desktop (JVM)**: Minimalist system tray status indicator displaying an operational health dot, hover metrics tooltip, and global shortcut overlay (Command Palette via `Cmd/Ctrl + K`).
- **Android**: Edge-to-edge transparent system bars and optional foreground telemetry alert notifications.

---

## Context & Guidelines for AI Agents

**Read [`docs/CONVENTIONS.md`](./docs/CONVENTIONS.md) before writing any code.** It covers project layout, design token usage, composable API shape, previews, animation/performance rules, Kotlin style, and the per-task workflow (showcase update → build verification → `PLAN.md` checkbox). Do not create git commits.

The essentials:
- Take every color, font, shape, and spacing value from `AppTheme`. Never hardcode hex, `dp`, or `sp` values.
- Put one public composable per file under `ui.common.components/<group>/`, each with its own `private @Preview`.

---

## Build & Run Commands

- **Build all targets**:
  ```bash
  ./gradlew assemble
  ```
- **Run Desktop App**:
  ```bash
  ./gradlew :desktopApp:run
  # Or with hot reload:
  ./gradlew :desktopApp:hotRun --auto
  ```
- **Build Android App**:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
- **Run Tests**:
  ```bash
  ./gradlew :shared:jvmTest
  ./gradlew :shared:testAndroidHostTest
  ```