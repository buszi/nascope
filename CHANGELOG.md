# Changelog

All notable changes to Nascope. The format follows [Keep a Changelog](https://keepachangelog.com/), versions follow [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-11-18

Your dashboard, your order: choose which bricks to show and arrange them.

### Added

- **Customize the dashboard:** a new editor, opened from the button next to REFRESH, lists every brick with a switch and a drag handle. Turn bricks off that you do not need and drag the rest into the order you want; the dashboard follows each change at once. On tablets and desktops the editor opens next to the live dashboard, on phones it fills the screen.
- The order and the bricks turned off are kept on the device, survive a restart and apply to every server. "Reset to default" (it asks first) brings back the built-in order with every brick on.
- Reordering without a mouse or touch: Alt+↑ / Alt+↓ on a focused handle, and "Move up" / "Move down" actions for screen readers.
- When every brick is turned off, the dashboard says so and offers a button to open the editor.

### Changed

- Switches use Material Design's motion (the thumb grows when on and when pressed, a highlight on hover and focus), in the app's colors. They are a little larger than before.
- A brick that is turned off costs nothing: it is not shown, and its data is not requested from the server. The "N ALERTS" chip in the header stays when the Alerts brick is off.

### Known limitations

- The layout is the same for every server; there are no presets and no layout per screen size.
- Bricks keep their designed size; only their order and visibility can be changed.
- The Windows and Linux packages are not code signed (SmartScreen asks once) and not in a package manager yet (winget, Scoop, apt repository, Flatpak).
- Before the deb is installed, GNOME Software shows its license as "unknown" and no icon: it reads neither from a package file. Both show once the app is installed.
- On Windows the first launch after installing or updating takes several seconds (Microsoft Defender checks the new files); later launches are quick.
- A certificate from a public authority reached under a name it does not cover (e.g. by IP) is a plain TLS error, with no offer to trust it.
- Bricks that need extra Glances configuration (service checks, S.M.A.R.T. disk health) are not included.
- No notifications, system tray or command palette yet.
- The macOS build is not notarized and runs on Apple Silicon only.

## [1.1.1] - 2026-11-16

Polish for the desktop apps after the first Windows and Linux packages.

### Added

- Windows: the "Installed apps" entry (Programs and Features) links the project website, the setup notes and the latest release.

### Changed

- Desktop: the window cannot be resized below 400 × 600, where the layout would no longer fit.

### Fixed

- Windows and Linux: the title bar is dark like the rest of the app instead of white (Windows 11 matches the app's top bar exactly, Windows 10 uses its own dark gray; on Linux it needs a window manager that honors GTK's dark variant, such as GNOME).
- Desktop: the window no longer shows a light strip along its bottom edge while it opens.
- Linux: the deb links the project website and has a full description in the software installer, and the installed app is listed in GNOME Software / KDE Discover with its icon and license. The package's maintainer field no longer repeats the name (`Nascope <Nascope <...>>`).

### Known limitations

- The Windows and Linux packages are not code signed (SmartScreen asks once) and not in a package manager yet (winget, Scoop, apt repository, Flatpak).
- Before the deb is installed, GNOME Software shows its license as "unknown" and no icon: it reads neither from a package file. Both show once the app is installed.
- On Windows the first launch after installing or updating takes several seconds (Microsoft Defender checks the new files); later launches are quick.
- A certificate from a public authority reached under a name it does not cover (e.g. by IP) is a plain TLS error, with no offer to trust it.
- Bricks that need extra Glances configuration (service checks, S.M.A.R.T. disk health) are not included.
- No notifications, system tray, command palette or dashboard customization yet.
- The macOS build is not notarized and runs on Apple Silicon only.

## [1.1.0] - 2026-11-13

### Added

- **Windows and Linux packages:** `Nascope-<version>.msi` (x64, per-user install) and `nascope_<version>_amd64.deb`, built and released next to the dmg and the APK.
- Trust a server's own certificate: when an HTTPS server's certificate is not trusted (self-signed, private authority, expired), the connection test shows its fingerprint, subject, issuer and expiry and asks whether to trust it, for that server's address only. A certificate that later changes is reported ("UNTRUSTED CERT") and asked about again, never accepted silently.

### Changed

- Bricks that fail on a missing password or an untrusted certificate offer "Enter password" / "Review certificate", which opens the server's editor, instead of a reconnect button that could not help.
- A server whose password is not stored on this device (restored from a backup, moved to another device, cleared keyring) now shows "NO PASSWORD" instead of "unauthorized"; the node card offers "Enter password" and the editor opens with the password field focused.

### Fixed

- Text fields stay above the on-screen keyboard: the window no longer pans (which hid the top bar and, on a landscape tablet, left an empty screen above the keyboard); the form scrolls and the focused field moves into view.
- Certificate fingerprints wrap between pairs instead of inside them.

### Known limitations

- The Windows and Linux packages are not code signed (SmartScreen asks once) and not in a package manager yet (winget, Scoop, apt repository, Flatpak).
- A certificate from a public authority reached under a name it does not cover (e.g. by IP) is a plain TLS error, with no offer to trust it.
- Bricks that need extra Glances configuration (service checks, S.M.A.R.T. disk health) are not included.
- No notifications, system tray, command palette or dashboard customization yet.
- The macOS build is not notarized and runs on Apple Silicon only.

## [1.0.0] - 2026-11-13

The first release: an MVP for **Android** and **macOS** (Apple Silicon).

### Added

- Connection setup: welcome screen, a guided check of the host, the Glances API and authentication, several servers ("nodes"), HTTP and HTTPS, optional basic authentication, demo mode.
- Live dashboard of bricks with a refresh interval setting, stale and offline states, and polling that pauses in the background: system overview, CPU (per core), memory and swap, GPU, storage, network, sensors, containers, processes, alerts, with a header badge for active alerts.
- Details screens: CPU cores, storage mounts and per-disk I/O (IOPS, latency), network interfaces, sensors, containers, processes and programs (search, sort, filters, paging, pinning), alerts and their history.
- Bricks that depend on an optional Glances plugin (GPU, alerts) are hidden when the connected server does not have it.
- Adaptive layouts for phones, tablets and desktop windows.
- Android: signed release build with R8 shrinking. Desktop: shrunk release package, icons, owner-only data directory.
- Passwords in the Android Keystore or the OS keyring; HTTPS certificate validation; no redirects followed.

### Known limitations

- No "trust this certificate" option for servers with self-signed certificates yet (see README, Security notes).
- A server profile restored on another device (or after the keyring was cleared) has no password and shows an "unauthorized" status instead of asking for it.
- Bricks that need extra Glances configuration (service checks, S.M.A.R.T. disk health) are not included.
- No notifications, system tray, command palette or dashboard customization yet.
- The macOS build is not notarized and runs on Apple Silicon only. Windows and Linux packages are not part of this release.
