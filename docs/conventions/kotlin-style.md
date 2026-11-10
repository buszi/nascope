# Kotlin Style Conventions

Language conventions and formatting guidelines for Nascope.

---

## 1. Formatting & Spacing

- **Indentation**: 4 spaces (no tabs).
- **Trailing Commas**: Always use trailing commas in multi-line parameter lists, argument lists, and collection literals.
- **Named Arguments**: Use named arguments for parameters beyond the first self-evident parameter, and always for theme/style overrides.
- **Imports**:
  - Explicit imports only.
  - Wildcard imports (`import foo.*`) are strictly prohibited.
  - Fully-qualified names inline (e.g. `androidx.compose.foundation.BorderStroke(...)`) are prohibited; import symbols explicitly.

---

## 2. Naming Conventions

- `PascalCase` for classes, interfaces, objects, and `private const val` constants (e.g., `PulseDurationMillis`, `DefaultColumnCount`).
- `camelCase` for functions, properties, parameters, and local variables.
- Mark immutable Compose data classes with `@Immutable` or `@Stable`.
