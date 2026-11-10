# Task Workflow Conventions

Workflow rules for completing self-contained tasks in Nascope.

---

## 1. Per-Task Execution Steps

For every task from `PLAN.md`:

1. **Implement deliverables**: Follow the design tokens and architecture rules described in the convention docs.
2. **Showcase Integration**: If adding or replacing a reusable component, integrate it into the showcase (`App.kt`) and delete any ad-hoc private implementations it supersedes.
3. **Verify the Build**:
   ```bash
   ./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain   # fast verification
   ./gradlew assemble                                              # full multiplatform build
   ./gradlew :shared:jvmTest                                       # run unit tests
   ```
4. **Update Documentation**: Update the relevant conventions or README files if a new token, convention, or module was introduced.
5. **Tick the Task Checkbox**: Mark the completed task checkbox in `PLAN.md`.

---

## 2. Working Tree & Commit Rule

- **Do NOT run `git commit`.**
- Always leave all changes uncommitted in the working tree for the user to review, verify, and commit.
