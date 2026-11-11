# Module Architecture

How Nascope is split into Gradle modules, what each layer may contain, and which way dependencies point.

If the code and this document disagree after a review correction, fix this document in the same change.

---

## 1. Modules & Dependency Direction

| Module | Package | Role | Project dependencies |
| :--- | :--- | :--- | :--- |
| `:domain-model` | `io.buszi.nascope.domain.model` | Shared domain models. Data only. | none |
| `:network` | `io.buszi.nascope.network` | Remote data access (Ktor, Glances HTTP API). | `:domain-model` |
| `:storage` | `io.buszi.nascope.storage` | Local persistence and secure storage. | `:domain-model` |
| `:domain` | `io.buszi.nascope.domain` | Business logic: use cases and their supporting classes. | `:domain-model` (as `api`), `:network`, `:storage` |
| `:shared` | `io.buszi.nascope` | UI: screens, ViewModels, navigation, design system, Koin application root. | `:domain`, `:resources` |
| `:resources` | `io.buszi.nascope.resources` | Strings, drawables, fonts. | none |

```
:shared ──> :domain ──> :network ──┐
                │                  ├──> :domain-model
                └─────> :storage ──┘
```

- Dependencies point only in the direction of the arrows. `:network` and `:storage` never depend on each other, on `:domain` or on `:shared`.
- `:shared` does not declare a dependency on `:network` or `:storage` and never imports their types. Everything the UI needs goes through `:domain`.
- All modules are Kotlin Multiplatform libraries (Android + JVM) built with `plugins.kmp.library`. Only `:shared` and `:resources` apply the Compose plugin.

---

## 2. `:domain-model`

- Contains models that more than one module needs: entities, value classes, enums and sealed result/failure types (e.g. `ServerProfile`, `ServerCredentials`, `ConnectionFailure`).
- Plain Kotlin only: no Koin, no Ktor, no serialization annotations, no coroutines, no logic beyond trivial derived properties.
- A model belongs here only if it crosses a module boundary as a parameter or return type of a `:network` / `:storage` interface, or is shared by several use cases. Models produced by one use case do **not** belong here (see section 4).

---

## 3. Data modules: `:network` and `:storage`

- Each data module **declares its own public interfaces** and implements them with `internal` classes wired into DI with Koin annotations. Nothing but the interfaces (and the module's Koin module class) is public.
- Interface parameters and return types are **domain models** from `:domain-model` or Kotlin/coroutines primitives. DTOs, Ktor types, DataStore types, exceptions from the underlying library and serialization annotations never leave the module. Map to domain models (including failures) at the module boundary.
- Data modules are **thin**: one interface function is one remote call or one storage operation. No business rules, no orchestration of several steps, no knowledge of what the UI does. If an implementation starts deciding *what should happen next*, that logic belongs in a use case.
- Data modules know nothing about each other. Anything that needs both (e.g. a request that needs a stored secret) is combined in `:domain`.
- Naming: describe the capability, e.g. `ServerProfileStore`, `SecretStore`, `GlancesConnectionClient`. Implementations are `internal class <Technology><Interface>` (e.g. `DataStoreServerProfileStore`, `KtorGlancesConnectionClient`).
- Platform-specific implementations live in `androidMain` / `jvmMain`; the interface stays in `commonMain`.

---

## 4. `:domain`

- Consumes the public interfaces of `:network` and `:storage` and exposes its own public API the same way: **public interfaces, `internal` implementations, wired with Koin**.
- Knows nothing about HTTP, files, keystores or any library used by the data modules.

### Use cases

- One use case is one business operation, in one file:
  ```kotlin
  fun interface SetActiveServerUseCase {
      suspend operator fun invoke(id: ServerProfileId)
  }

  @Factory
  internal class SetActiveServerUseCaseImpl(
      private val profileStore: ServerProfileStore,
      private val preferencesStore: AppPreferencesStore,
  ) : SetActiveServerUseCase {
      override suspend fun invoke(id: ServerProfileId) { /* ... */ }
  }
  ```
- `@Factory` by default. Use `@Single` only when the implementation holds shared state (e.g. one polling loop shared by all collectors).
- A use case may call other use cases through their interfaces.
- Business rules and multi-step orchestration live here, not in ViewModels and not in data modules.

### Other domain classes

- Supporting classes that make sense on their own (validators, policies, calculators, shared state holders) are allowed. Same rule: public interface only if `:shared` needs it, otherwise `internal`.

### Where a model goes

| The model is... | Declare it in |
| :--- | :--- |
| used by a `:network` / `:storage` interface, or shared by several use cases | `:domain-model` |
| the input or result of a single use case, produced by that use case's own transformation | nested inside that use case's interface |

```kotlin
fun interface ObserveConnectionStatusUseCase {
    operator fun invoke(): Flow<Status>

    sealed interface Status {
        data object Idle : Status
        data object Connecting : Status
        data object Online : Status
        data class Offline(val failure: ConnectionFailure) : Status   // ConnectionFailure is shared -> :domain-model
    }
}
```

Start with the nested form. Move a model to `:domain-model` only when a second consumer outside the use case's own callers actually needs it.

---

## 5. `:shared` (UI)

- ViewModels inject **use case interfaces** (and other public `:domain` interfaces). They never inject a `:network` or `:storage` type.
- Domain models (from `:domain-model` or nested in a use case) never appear in a `ViewState`, `ViewAction`, composable signature or preview. Map them to UI models in `ui/common/model` at the ViewModel boundary (see `viewmodel-mvi.md`, section 1a).

---

## 6. Dependency Injection

- `:network`, `:storage` and `:domain` each declare one Koin module class (`@Module @Configuration @ComponentScan("<module package>")`) and apply the Koin compiler plugin.
- The root `@KoinApplication` lives in `:shared`. Koin is started (together with Napier's `DebugAntilog`) by `initNascope()` from `:shared`, called once by each platform entry point before any UI exists: `NascopeApplication.onCreate` on Android, the first line of `main` on desktop. `App()` does not start Koin.
- **Cross-module discovery works without `includes`**: the Koin compiler plugin emits discovery hints for each `@Configuration` module, and `koinConfiguration<NascopeApp>()` resolved `:storage` definitions (verified in TASK-4.2). Do not wire modules with `@Module(includes = ...)`.
- Platform-specific definitions are annotated classes in `androidMain` / `jvmMain` (e.g. `AndroidStorageDirectory`, `JvmStorageDirectory`) implementing an `internal` common interface; the common module class consumes that interface. Where the choice depends on availability (secret store), use an `internal expect fun` returning `null` when unavailable and fall back in common code.
- Platform objects (e.g. the Android `Context`) are registered by the platform entry point through the `platformConfiguration` parameter of `initNascope()` (a `KoinAppDeclaration`, default empty); modules receive them by constructor injection. `NascopeApplication` registers `single<Context> { applicationContext }`; `desktopApp` passes nothing.

---

## 7. Tests

- Each module tests its own code in its own `src/jvmTest/kotlin`.
- `:domain` tests use hand-written fakes of the data module interfaces; they never start Ktor or DataStore.
- `:network` tests use Ktor `MockEngine`; `:storage` tests use temporary files and in-memory fakes of platform services.
- `TestCoroutineRule` exists only in `:shared`; elsewhere use `runTest` directly.
