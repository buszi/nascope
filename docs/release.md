# Release runbook

Work in progress: built up task by task in Phase 6B of `PLAN.md`. This version covers Android signing and desktop packaging.

## Android release signing

The release APK is signed with the project's own release key. There is **no fallback to the debug key**: without the secrets below, `assembleRelease` still works but produces an *unsigned* APK (`app-release-unsigned.apk`, not installable) and prints a warning when it is packaged.

The release key is a one-off asset that lives **outside the repository** (`*.jks`, `*.keystore` and `keystore.properties` are git-ignored only as a safety net). If it is lost, existing installs can never be updated, so keep a backup of the keystore **and** both passwords somewhere that is not the build machine.

- Backup location: kept privately by the owner, deliberately not recorded in the repository.
- Certificate SHA-256 (published in the README so users can compare it with the APK they downloaded): `37:B1:BD:A5:AD:60:02:0C:B0:8F:05:67:65:F4:02:F0:AC:0C:90:34:C8:AA:20:42:50:22:79:E3:76:00:CA:E2`

### Inputs

Four values, read from the **environment first**, then from Gradle properties:

| Name | Meaning |
|---|---|
| `NASCOPE_KEYSTORE_FILE` | path to the `.jks` (a relative path is resolved against the repository root) |
| `NASCOPE_KEYSTORE_PASSWORD` | keystore password |
| `NASCOPE_KEY_ALIAS` | key alias |
| `NASCOPE_KEY_PASSWORD` | key password |

If any is missing, or the file does not exist, the release build is unsigned and the warning names what is missing.

### Local release build

Put the four values in `~/.gradle/gradle.properties` (never in the repository), then:

```sh
./gradlew :androidApp:assembleRelease
```

### CI

The release workflow (TASK-6B.5) takes the same four values from repository secrets; the keystore itself is stored base64-encoded and decoded into a file on the runner. The `versionCode` comes from the `VERSION_CODE` environment variable (the CI run number), `1` locally.

### Verifying a signed APK

```sh
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs \
  androidApp/build/outputs/apk/release/androidApp-release.apk
```

The certificate must be the release key, not `CN=Android Debug`.

## Desktop packages

One installer per OS, each built **on that OS** (jpackage cannot cross-compile; CI uses a runner per OS, TASK-6B.5):

```sh
./gradlew :desktopApp:packageReleaseDistributionForCurrentOS
# desktopApp/build/compose/binaries/main-release/{dmg,msi,deb}/
```

Names come from `DesktopApplicationPlugin`: the app is **Nascope** (`Nascope.app`, "Nascope" in the Start menu), bundle id `io.buszi.nascope`, files `Nascope-<version>.dmg` and `.msi`, and a `.deb` (its exact file name is confirmed on the first Linux build; the release workflow and the Homebrew / winget steps depend on these names).

- **Version:** `app-version` in `gradle/libs.versions.toml` is the `packageVersion`. It must be plain `MAJOR.MINOR.PATCH` with a major of at least 1 (macOS). A release candidate such as `v1.0.0-rc1` is therefore only a git tag and a GitHub pre-release; the files inside it are still `1.0.0`.
- **Windows upgrades:** `upgradeUuid` in the plugin identifies the product. A newer MSI upgrades an older one in place only while it stays the same, so **never change it**. The MSI installs per user (no administrator prompt).
- **Debian maintainer:** the `Maintainer` field needs `Name <email>`; nothing validates the address. Releases take it from the `NASCOPE_DEB_MAINTAINER` environment variable (a repository secret in CI; the release workflow must pass it through) or a Gradle property. Local builds use a placeholder, so never publish a locally built deb.
- **Runtime modules:** the packaged app ships a jlinked JRE with an explicit module list. After adding or upgrading a dependency run `./gradlew :desktopApp:suggestRuntimeModules` and update `modules(...)` in the plugin; a missing module only fails at runtime.
- **macOS signing:** packages are ad-hoc signed by jpackage and not notarized (see the install notes in the README, TASK-6B.7).

### Checking a package

On the machine of the package's OS, install it (macOS: copy `Nascope.app` out of the dmg) and check:

1. The name, icon and menu bar / Start menu entry say "Nascope".
2. It starts and stays up (no console window on Windows).
3. Add the NAS over HTTPS and see the dashboard, open a details screen, delete the profile (proves the packaged runtime has what networking, TLS and the keyring need).
4. Windows: installing a newer MSI over an older one upgrades in place (one entry in "Installed apps").
5. Linux: `sudo apt install ./nascope_*.deb` installs, the launcher appears in the menu, `sudo apt remove nascope` removes it.

### Desktop hardening decisions and notes

- **Shrinking (ProGuard) is on for the release packages.** It applies to the `*Release*` tasks only (`packageReleaseDistributionForCurrentOS`, `createReleaseDistributable`); the plain `package*` tasks build the unshrunk variant. `-Pnascope.desktop.shrink=false` turns it off. It saves about 12% of the installed app (153 MB to 134 MB; the app jars go from 71 MB to 52 MB, the rest is the JRE and Skia). Unlike R8, Compose Desktop's ProGuard ignores the consumer rules shipped in dependencies, so what libraries load through service files or reflection is kept by hand in `desktopApp/proguard-rules.pro` (OkHttp `dontwarn`s, serialization, the coroutines Swing dispatcher, DataStore's protobuf, JNA and the keyring backends, Ktor's JSON provider). Only obfuscation and optimization are off.
  - **How it was verified (macOS, shrunk build):** all 62 generated serializers are kept; the first attempt lost Ktor's JSON provider and was fixed; the owner then ran the shrunk app by hand: it loaded the saved profile from storage, polled the NAS (478 requests, all `200`, no exception in the debug log), opened details screens, edited a profile and went through onboarding on a fresh data directory.
  - **Not verified:** Windows and Linux shrunk builds (the Credential Manager and Secret Service keyring backends, the D-Bus classes that are built reflectively; their classes are kept by rule). The first Windows / Linux release candidate from CI must go through the check list above before a release; if one of them breaks, build that OS with `-Pnascope.desktop.shrink=false` and fix the rule.
  - **After a dependency change:** a missing rule fails at runtime, not at build time. Build a release package, launch it, connect to a server and open the details screens.
- **Data directory:** `~/Library/Application Support/nascope` (macOS), `%APPDATA%\nascope` (Windows), `$XDG_CONFIG_HOME` or `~/.config/nascope` (Linux). It holds the profile list (hosts, ports, user names) and the preferences, never a password. On macOS and Linux the directory is created and re-tightened to owner-only (`rwx------`).
- **Secrets:** passwords live in the OS keyring (macOS Keychain, Windows Credential Manager, Linux Secret Service), service `io.buszi.nascope`, account = profile id. If there is no keyring, or it refuses a write (locked, access denied), the password is kept in memory for the session and the editor shows the "password not saved" notice, instead of failing. After a restart such a profile has no password (see the "Missing password" item in the plan's TODO).
- **TLS:** certificates are validated with the JVM's default trust store, there is no trust-all code. A server with a self-signed certificate fails with the "TLS error" message; **there is no per-server "trust this certificate" option yet**, so users with a private CA must install it in the OS / JVM trust store, or use HTTP on a trusted LAN. Document this in the README (TASK-6B.6); an opt-in per-server exception is a post-MVP feature.
- **Logging:** a packaged app logs warnings and errors only (TASK-6B.2); SLF4J prints three "no provider" lines at startup, which is harmless.
- **Not audited:** whether the packaged app writes anywhere besides the data directory (for example temporary files); the Windows installer shows no console window (Compose's default `winConsole = false`, to be seen on the first Windows build).

