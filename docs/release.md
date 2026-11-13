# Release runbook

How Nascope is built, signed, released and distributed. Start with the checklist; the sections after it hold the detail.

## Release checklist

Everything in this file is the detail behind these steps. A release is a git tag; the workflow does the rest.

**Before tagging**

1. Decide the version (`MAJOR.MINOR.PATCH`) and set `app-version` in `gradle/libs.versions.toml`. The tag must be `v` plus exactly this version (`v1.0.1` for `1.0.1`); the workflow refuses anything else.
2. Update `CHANGELOG.md`: move the entries under the new version with today's date, and keep "Known limitations" honest.
3. Run the full verification locally: `./gradlew assemble :domain-model:jvmTest :storage:jvmTest :network:jvmTest :domain:jvmTest :shared:jvmTest`.
4. If the UI or storage changed, do a device pass: the `nascope-qa` skill on the three emulators, and a packaged desktop build against a real server (the check list under "Desktop packages").
5. Commit and push `main`, and wait for the `CI` workflow to be green **on that commit**. Tag only a commit that CI has passed.
6. Check the repository secrets exist (Settings, Secrets and variables, Actions): `NASCOPE_KEYSTORE_FILE`, `NASCOPE_KEYSTORE_PASSWORD`, `NASCOPE_KEY_ALIAS`, `NASCOPE_KEY_PASSWORD`, `HOMEBREW_TAP_TOKEN`. The keystore itself must still be backed up (see "Android release signing").

**Tagging**

```sh
git tag v1.0.0 && git push origin v1.0.0
```

For a rehearsal use a suffix, `v1.0.0-rc1`: it is published as a pre-release and never reaches Homebrew. Watch the `Release` workflow in the Actions tab (about 15 to 20 minutes: Android first, then macOS, then publish, then the tap).

**After the workflow**

1. The Release page has `Nascope-<version>.apk`, `Nascope-<version>.dmg` and `SHA256SUMS`, and is marked "Latest" (or "Pre-release" for a suffix).
2. Download the files and check them: `shasum -a 256 -c SHA256SUMS`, and `apksigner verify --print-certs Nascope-<version>.apk` shows the certificate fingerprint from the README.
3. Install the APK on a device or emulator and start it; open the dmg (or `brew install --cask buszi/tap/nascope`), go through the first launch and connect to a server.
4. A real release (no suffix) added a commit "nascope <version>" to `buszi/homebrew-tap`; `brew update && brew upgrade --cask nascope` picks it up.
5. Announce it.

**When something goes wrong**

- **The workflow fails before the `publish` job:** nothing is public. Fix the cause, then delete the tag and tag again: `git push --delete origin v1.0.0 && git tag -d v1.0.0`, push the fix, create the tag on the new commit. (The Android job runs first; the macOS job only starts when it passed.)
- **The `publish` job succeeded but the release is wrong:** a published tag is never moved. Ship a fixed patch version (`1.0.1`); mark the bad release as a pre-release or delete it on GitHub, and tell users which version to take.
- **Only the `homebrew` job failed:** the release itself is fine. Fix the cause (usually the token) and re-run that job from the Actions tab, or edit `version` and `sha256` in the tap by hand.
- **The keystore is lost:** installed apps can never be updated again; users must uninstall and lose their saved servers. This is why the backup matters.

**Later (not part of 1.0.0, so nobody re-decides it)**

- Windows (`.msi`, winget, Scoop) and Linux (`.deb`, an apt repository, Flatpak): add them after hand testing (see "Adding an OS").
- Apple notarization (a paid Developer ID) would remove the Gatekeeper steps and allow the official Homebrew cask.
- Google Play (a paid account, an AAB: switch `bundleRelease` on in the workflow), F-Droid (needs a reproducible build).
- Windows code signing (SignPath is free for open source).
- An in-app "new version available" check against the GitHub API.
- An Intel Mac build (an Intel runner row).

---

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

The release workflow takes the same four values from repository secrets; the keystore itself is stored base64-encoded and decoded into a file on the runner (see "CI and releases"). The `versionCode` comes from the `VERSION_CODE` environment variable (the CI run number), `1` locally.

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

## CI and releases

Two workflows in `.github/workflows`:

- **`ci.yml`** (pull requests and pushes to `main`, skipped for docs-only changes): `./gradlew assemble` and the module tests on one Ubuntu runner. A newer push cancels the older run.
- **`release.yml`** (a pushed tag `vX.Y.Z`, or `vX.Y.Z-rc1` for a pre-release): 1. checks that the tag matches `app-version` (a `-rcN` suffix is allowed), runs the tests, builds and signs the APK on Ubuntu and checks that its certificate is the release key (the fingerprint is `EXPECTED_CERT_SHA256` in the workflow); 2. only then builds the macOS dmg (macOS minutes are the costly ones, so a failing test never starts that runner); 3. publishes a GitHub Release with `Nascope-<version>.apk`, `Nascope-<version>.dmg` and `SHA256SUMS`, with generated notes, as a pre-release when the tag has a suffix.

### Repository secrets (Settings, Secrets and variables, Actions)

| Secret | Value |
|---|---|
| `NASCOPE_KEYSTORE_FILE` | the release keystore, base64-encoded (`base64 -i nascope-release.jks \| pbcopy`); the workflow decodes it to a file and gives Gradle that path in the same-named variable |
| `NASCOPE_KEYSTORE_PASSWORD` | keystore password |
| `NASCOPE_KEY_ALIAS` | key alias |
| `NASCOPE_KEY_PASSWORD` | key password |
| `NASCOPE_DEB_MAINTAINER` | only when the deb is built again (see "Adding an OS") |

### Cutting a release

```sh
git tag v1.0.0-rc1 && git push origin v1.0.0-rc1   # a rehearsal: published as a pre-release
git tag v1.0.0 && git push origin v1.0.0            # the real one
```

A bad release is superseded by a new patch version; a published tag is never moved. Delete a failed rehearsal's pre-release and tag by hand before reusing the name.

### Notes

- The macOS dmg is built on an Apple Silicon runner, so it runs on Apple Silicon Macs only (the bundled runtime is architecture specific). An Intel build needs an Intel runner row.
- Actions are pinned to major versions (`@v4`), not commit SHAs.
- Artifacts between jobs are kept for 7 days.

### Adding an OS

Windows (`.msi`) and Linux (`.deb`) are not built yet: they are added after they have been tested by hand. Add a row to the `matrix.include` list of the `desktop` job in `release.yml`, for example:

```yaml
- name: Windows
  runner: windows-latest
  task: packageReleaseMsi
  folder: msi
- name: Linux
  runner: ubuntu-latest
  task: packageReleaseDeb
  folder: deb
```

The Linux row also needs `NASCOPE_DEB_MAINTAINER` passed to the build as an environment variable (the secret). Check the artifact names afterwards: the Homebrew and winget steps (TASK-6B.7) depend on them.

## Distribution channels

The GitHub Release is the source of truth; every other channel only points at its files.

### Homebrew tap (macOS)

Users install with `brew install --cask buszi/tap/nascope`. The cask lives only in a separate repository, **`buszi/homebrew-tap`** (the `homebrew-` prefix is required), at `Casks/nascope.rb`; edit it there. This repository does not keep a copy.

Set up once (done): the public repository `buszi/homebrew-tap` with `Casks/nascope.rb`, and a fine-grained personal access token limited to that repository with **Contents: read and write**, stored as the repository secret `HOMEBREW_TAP_TOKEN` of this repository. Without the secret the `homebrew` job only prints a warning.

After that every release tag without a suffix (`v1.0.0`, not `v1.0.0-rc1`) makes the `homebrew` job of `release.yml` write the new `version` and the dmg's `sha256` to the tap. Release candidates never reach Homebrew.

Things to know:

- **Gatekeeper.** The app is ad-hoc signed and not notarized. Homebrew removed the `--no-quarantine` option (Homebrew 5.0.0) and, since 2026-09-01, its *official* cask repository disables unsigned casks, so Nascope cannot be listed there; a third-party tap is not subject to that. The cask removes the quarantine attribute in a `postflight` block (`xattr -dr com.apple.quarantine`), which works technically but is **not an officially documented Homebrew practice**: test it on a clean Mac with a release candidate's dmg (`brew install --cask ./nascope.rb` with its real `sha256`, then launch) and keep the manual "Open Anyway" steps in the README as the fallback. Revisit this if Homebrew changes how it treats third-party casks.
- The dmg is Apple Silicon only, hence `depends_on arch: :arm64`.
- `brew upgrade` follows the tap: it sees a new version only after the `homebrew` job has run.

### Android

The APK on the GitHub Release is the install route. [Obtainium](https://obtainium.imranr.dev/) can follow the repository's releases for update notifications. There is no store listing; see "Later" in the runbook for Play and F-Droid.

### Not yet

winget, Scoop, an apt repository and Flatpak wait for the Windows and Linux builds (see "Adding an OS").

