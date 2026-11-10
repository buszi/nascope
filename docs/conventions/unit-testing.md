# Unit Testing Conventions (ViewModels & State)

Conventions for unit testing ViewModels, state holders, and presentation logic in Nascope.

---

## 1. Test Location & Engine

- All ViewModel and state tests live in `shared/src/jvmTest/kotlin/` matching the package structure of the code under test.
- Using JVM host tests ensures fast execution and full support for Mockk bytecode manipulation.

---

## 2. Structure & Testing Patterns

### A. Coroutine Rule
- Attach `@get:Rule val coroutineRule = TestCoroutineRule()` to every ViewModel test class.
- `TestCoroutineRule` sets `Dispatchers.Main` to its test dispatcher before each test and resets it afterwards.
- It also exposes `coroutineRule.dispatcher` for tests requiring explicit dispatcher injection.

### B. Mocks & Subject Under Test
- Declare mocks as private properties at the top of the test class: `private val dependency = mockk<Dependency>()`.
- Instantiate the subject under test lazily:
  ```kotlin
  private val viewModel by lazy {
      SystemOverviewViewModel(telemetryStateHolder = telemetryStateHolder)
  }
  ```

### C. StateFlow Testing with Turbine
- Wrap test logic in `runTest { turbineScope { ... } }`.
- Observe emissions using Turbine's `testIn(this)`:
  ```kotlin
  val viewStateTurbine = viewModel.viewState.testIn(this)
  ```
- If the ViewModel launches background collection in `viewModelScope`, call `runCurrent()` to advance the coroutine scheduler.
- Assert the emitted state list:
  ```kotlin
  assertEquals(
      listOf(initialViewState, readyViewState),
      viewStateTurbine.cancelAndConsumeItems(),
  )
  ```

### D. Verification
- Verify mock call sequences using Mockk's `verifySequence { ... }`.

### E. Test Fixtures in Companion Object
- Place domain models, sample telemetry payloads, initial view states, and expected ready states in a `companion object` placed **at the end of the test file**:
  ```kotlin
  companion object {
      private val hostOverview = HostOverviewData(...)
      private val initialViewState = ViewState()
      private val readyViewState = ViewState(...)
  }
  ```

---

## 3. Example Test Template

```kotlin
package io.buszi.nascope.ui.dashboard.widgets.sample

import app.cash.turbine.turbineScope
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val repository = mockk<SampleRepository>()

    private val viewModel by lazy {
        SampleViewModel(repository = repository)
    }

    @Test
    fun sampleViewModelEmitsExpectedStates() = runTest {
        turbineScope {
            val dataFlow = MutableStateFlow(sampleData)
            every { repository.data } returns dataFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                repository.data
            }
        }
    }

    companion object {
        private val sampleData = SampleData(id = "alpha-1")
        private val initialViewState = ViewState(widgetState = WidgetState.Loading)
        private val readyViewState = ViewState(data = sampleData, widgetState = WidgetState.Ready)
    }
}
```
