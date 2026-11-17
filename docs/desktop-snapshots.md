# Desktop Snapshots

`./gradlew :desktopApp:snapshot` renders the real desktop app **offscreen** and writes PNGs (and semantics dumps) driven by a small script. It needs no window and no macOS permission (no Screen Recording, no Accessibility): it only ever sees Nascope's own pixels. Use it for the "quick look on the desktop" a task asks for, at exact Compact / Medium / Expanded widths.

```bash
./gradlew :desktopApp:snapshot -Psnapshot.script=desktopApp/snapshots/dashboard-widths.snap
# PNGs in desktopApp/build/snapshots/
```

| Property | Meaning |
| :--- | :--- |
| `snapshot.script` (required) | The script to run. Relative paths start at the repository root. |
| `snapshot.out` | Where PNGs and `.txt` dumps go. Default `desktopApp/build/snapshots`. |
| `snapshot.home` | The scratch home directory the app stores its settings in. Without it a fresh `<out>/home` is used (wiped first). Pass the same one to two runs to check that something survives an app restart. |
| `snapshot.density` | Pixels per dp in the images. Default `1`. |

The exit code is `0` when every command ran, `1` when one failed (the line, the command and the reason are printed; the shots taken so far stay), `2` for a script that cannot be read, `3` when a command hung and the watchdog stopped the run.

---

## Isolation

- The app runs with `user.home` pointed at the scratch home, so `JvmStorageDirectory` (preferences, server profiles) lives there. The run refuses to start against the real home directory.
- The OS keychain is replaced by an in-memory `SecretStore` (`InMemorySnapshotSecretStore`); a password a script types never leaves the process.
- `java.awt.headless=true`: nothing appears on the screen or in the dock.
- Demo mode needs no network. Pointing a script at a real server is possible through the app's own setup flow, but snapshots are meant for demo data.

---

## Script

One command per line, `#` starts a comment. The full reference is the KDoc of `SnapshotScript`.

| Command | Does |
| :--- | :--- |
| `demo on` / `demo off` | Demo mode, through `SetDemoModeUseCase`. |
| `layout default` / `layout order=<keys> hidden=<keys>` | The stored dashboard layout, through `SetDashboardLayoutUseCase` (keys as in `DashboardBrickId.key`, comma-separated). |
| `start` | Shows the app (the first app command does it too). Put setup commands before it to avoid a first frame without them. |
| `size <w> <h>` | The app's size in dp. Compact below 768, Medium below 1280 (the grid adds the rail's width when it decides). |
| `wait <ms>` | Lets real time pass while frames are drawn: data loading, polling, animations. Give a fresh start about 2–3 s. |
| `shot <name>` | Writes `<out>/<name>.png`. |
| `tree <name>` | Writes the unmerged semantics tree (test tags, texts, states) to `<out>/<name>.txt`: the way to find what is on screen and what to click. |
| `click <tag>` / `clicktext <text>` | Clicks the one node with that test tag / containing that text. |
| `scrollto <container> <tag>` | Scrolls a scrollable container (e.g. `dashboard_grid`) until the tagged node exists, and brings it to the top. |
| `drag <tag> <dx> <dy>` | A mouse drag from the node's centre by dx / dy dp. |
| `key <name>` | `tab`, `enter`, `space`, `escape`, `up`, `down`, `left`, `right` on the focused node. |

Test tags are the ones in `TestTags` (the same the QA bench uses). Lazy lists compose only what is on screen: `click` and `tree` see nothing below the fold until `scrollto`, and a tall `size` (e.g. `1440 2400`) shows a whole dashboard in one shot.

---

## How it works, and its limits

- `desktopApp/src/snapshot/` is a source set of its own (never packaged; `assemble` compiles it so it cannot rot). It runs `App()` inside Compose's `runSkikoComposeUiTest` scene, sized to the largest `size` in the script, with the app in a box of the requested size.
- Frames are driven by hand (`mainClock.autoAdvance = false`) while real time passes, because the app's data arrives on real threads (polling, DataStore) and the pulsing status chips are infinite animations that never let a test scene go idle. Every lookup avoids the test library's implicit "wait for idle".
- Node lookups are checked by the harness with a short message: the library's own "expected exactly one node" error prints the whole tree per candidate and takes minutes. The library's `performScrollTo` can loop forever on the dashboard grid, so `scrollto` scrolls with the `ScrollBy` semantics action instead. A per-command watchdog ends any other hang.
- Not covered: the native window (title bar and macOS chrome, window resizing, minimum size, the first paint), real fonts' platform hinting, the system tray. Those still need a look at the running app.
