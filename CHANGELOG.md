# Changelog

All notable changes to Nascope. The format follows [Keep a Changelog](https://keepachangelog.com/), versions follow [Semantic Versioning](https://semver.org/).

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
