# Changelog

All notable changes to Nascope. The format follows [Keep a Changelog](https://keepachangelog.com/), versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-11-13

### Added

- **Windows and Linux packages:** `Nascope-<version>.msi` (x64, per-user install) and `nascope_<version>-1_amd64.deb`, built and released next to the dmg and the APK.
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
