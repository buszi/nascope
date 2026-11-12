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
| `:test-utils` | `io.buszi.nascope.test` | Shared JVM test helpers. Test dependency only. | none |
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

- Contains models that more than one module needs: entities, value classes, enums and sealed result/failure types (e.g. `ServerProfile`, `ServerCredentials`, `ConnectionFailure`). Packages: `domain.model.connection` (server profiles, connection input and failures), `domain.model.telemetry` (raw Glances snapshot models such as `CpuUsage` and `TelemetryFailure`: raw units, no history, no formatting; plus `TelemetryState`, the `Loading` / `Ready(data, isStale)` / `Failed(failure)` result of every observing telemetry use case) and `domain.model.settings` (`RefreshInterval`).
- Plain Kotlin only: no Koin, no Ktor, no serialization annotations, no coroutines, no logic beyond trivial derived properties.
- A model belongs here only if it crosses a module boundary as a parameter or return type of a `:network` / `:storage` interface, or is shared by several use cases. Models produced by one use case do **not** belong here (see section 4).

---

## 3. Data modules: `:network` and `:storage`

- Each data module **declares its own public interfaces** and implements them with `internal` classes wired into DI with Koin annotations. Nothing but the interfaces (and the module's Koin module class) is public.
- New fallible data calls return `com.github.michaelbull.result.Result<T, E>` with an error type from `:domain-model` (e.g. `Result<CpuUsage, TelemetryFailure>`). A module whose public interface uses it declares `kotlin-result` as `api`. Existing `ConnectionFailure?` APIs are not migrated.
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

- One use case is one business operation. The public `fun interface` (with its nested models) lives in `domain/<feature>/`; the `internal` implementation lives in its own file in the `impl` subpackage, `domain/<feature>/impl/<Name>UseCaseImpl.kt`:
  ```kotlin
  // domain/connection/SetActiveServerUseCase.kt
  fun interface SetActiveServerUseCase {
      suspend operator fun invoke(id: ServerProfileId)
  }

  // domain/connection/impl/SetActiveServerUseCaseImpl.kt
  @Factory
  internal class SetActiveServerUseCaseImpl(
      private val profileStore: ServerProfileStore,
      private val preferencesStore: AppPreferencesStore,
      private val dispatcher: CoroutineDispatcher,
  ) : SetActiveServerUseCase {
      override suspend fun invoke(id: ServerProfileId) = withContext(dispatcher) { /* ... */ }
  }
  ```
- **Threading is decided here, at the use case level.** `DomainModule` provides `Dispatchers.IO` as the `CoroutineDispatcher` in the DI tree. Every use case that touches a store or the network takes `private val dispatcher: CoroutineDispatcher` and switches eagerly: `withContext(dispatcher)` in suspend use cases, `.flowOn(dispatcher)` on the returned flow. Pure computation (e.g. validation) takes no dispatcher. Data modules and ViewModels do not pick dispatchers.
- `@Factory` by default. Use `@Single` only when the implementation holds shared state (e.g. one polling loop shared by all collectors).
- A use case may call other use cases through their interfaces.
- Business rules and multi-step orchestration live here, not in ViewModels and not in data modules.

### Shared state and long-running work

- A use case that must keep one loop alive for several collectors (e.g. `ObserveConnectionStatusUseCase`, which polls the active server) is a `@Single` that shares its flow with `shareIn(applicationScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)`: the loop starts with the first collector, stops with the last, and a new collector never sees a stale value.
- **Polling loops are gated by the app foreground state.** `domain/foreground/` holds the public `SetAppForegroundUseCase` (called only by `AppRootViewModel`) and the internal `AppForegroundState` (`@Single`). A `true` is published immediately; a `false` only after a 5 s grace period, so rotation or a quick app switch does not interrupt polling. A loop collects `isForeground` (`flatMapLatest`) so it makes no request in the background and checks immediately on return, as `ObserveConnectionStatusUseCaseImpl` does.
- `domain/settings/` holds the global app settings use cases (`ObserveRefreshIntervalUseCase`, `SetRefreshIntervalUseCase`); the value lives in `AppPreferencesStore` and `RefreshInterval` in `:domain-model`.
- `DomainModule` provides the application-level `CoroutineScope` (`SupervisorJob` + the IO dispatcher). Tests pass a scope built on `coroutineRule.dispatcher` and cancel it in `@After`, so loops run in virtual time.

### Other domain classes

- `domain/telemetry/` holds the telemetry data path:
  - `TelemetrySource` (one session's telemetry), `LiveTelemetrySource` (delegates to `GlancesTelemetryClient`, building the `ServerConnection` on every call via `ServerProfile.toServerConnection`), `DemoTelemetrySource` (seeded random walk, never fails) and `TelemetrySourceProvider` (a new source per session change). All `internal`. `:domain` depends on `kotlin-result` as `implementation`.
  - `TelemetryPoller` / `TelemetryPollerImpl` (`internal`, `@Single`): one shared polling loop per Glances plugin (`Polled` = `Loading` / `Value(value, isStale)` / `Failed`), tiers declared once in `TelemetryPlugin`.
  - Public observing use cases (`Observe<Feature>UseCase`, each `() -> Flow<TelemetryState<Data>>` with `Data` nested) are thin: they pick one **primary** plugin (the state follows it) and optional **secondary** plugins (a loading, failed or unavailable secondary only makes its own fields `null`). The rule is written once in `PolledStates.kt` (`toTelemetryState`). `RefreshTelemetryUseCase` calls `TelemetryPoller.refresh()`. `ObserveAvailablePluginsUseCase` (`() -> Flow<Set<String>?>`, `@Factory`) is the exception to the `TelemetryState` shape: it lists the plugins of the server's `pluginslist` (a `Static`-tier loop), `null` while loading or failed. Plugins that Glances lists but that may hold nothing (`ConfigDependentPlugins`, today `gpu`) are included only after their first successful poll returned entries, decided once per session. Use cases without history are `@Factory`. Use cases with history (`ObserveCpuUseCase`, `ObserveStorageUseCase`, `ObserveNetworkUseCase`, `ObserveSensorsUseCase`, `ObserveGpuUseCase`) are `@Single` and share their combined flow (`shareWhileObserved`), so a brick and its details screen see one history. History is kept in `MetricRingBuffer`s (60 samples, appended only for a fresh value, cleared on `Loading`; keyed by interface name / sensor label via `KeyedMetricHistory`), created per collection by `withHistory`. `SensorClassifier` groups sensors by label because Glances reports every NAS temperature as `temperature_core`.
  - Koin check (TASK-6.4): use case implementations whose constructor takes the `internal` `TelemetryPoller` resolve fine through `KoinGraphTest` in `:shared`; no `KOIN-D001`, no workaround needed.

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
- **Cross-module wiring uses `includes`** (settled in TASK-4.2 / 4.4): runtime discovery of `@Configuration` modules works, but the compile-time dependency check in `:shared` cannot see definitions of modules that are not on its compile classpath. So `DomainModule` is declared `@Module(includes = [NetworkModule::class, StorageModule::class])` and `AppModule` `@Module(includes = [DomainModule::class])`. A new data module must be added to `DomainModule`'s includes.
- **A definition's constructor may only take public types** (or be marked `@Provided`). The cross-module check cannot read definitions whose constructor needs an `internal` type from another module: the definition silently loses its interface binding and consumers fail with `KOIN-D001 Missing dependency`. Hence no `internal` interface between a data module's definitions; platform classes take `Context` (public, `@Provided` because it is registered at runtime) instead of an internal helper type.
- Stale Koin hints after changing definitions can produce a bogus `KOIN-D001`; re-run with `--rerun-tasks` before debugging.
- Platform-specific definitions are annotated classes in `androidMain` / `jvmMain` (e.g. `AndroidStorageDirectory`, `JvmStorageDirectory`) implementing an `internal` common interface; the common module class consumes that interface. Where the choice depends on availability (secret store), use an `internal expect fun` returning `null` when unavailable and fall back in common code.
- Platform objects (e.g. the Android `Context`) are registered by the platform entry point through the `platformConfiguration` parameter of `initNascope()` (a `KoinAppDeclaration`, default empty); modules receive them by constructor injection. `NascopeApplication` registers `single<Context> { applicationContext }`; `desktopApp` passes nothing.

---

## 7. Tests

- Each module tests its own code in its own `src/jvmTest/kotlin`.
- `:domain` tests follow `unit-testing.md`: `TestCoroutineRule`, Mockk mocks of the `:network` / `:storage` interfaces (never Ktor or DataStore), the rule's dispatcher passed to the use case (`dispatcher = coroutineRule.dispatcher`), Turbine for flows. One test class per implementation, in the `impl` package next to it.
- `:network` tests use Ktor `MockEngine`; `:storage` tests use temporary files and in-memory fakes of platform services.
- Shared test helpers (`TestCoroutineRule`, Turbine extensions) live in the `:test-utils` module (package `io.buszi.nascope.test`); modules add it as a `jvmTest` dependency.
