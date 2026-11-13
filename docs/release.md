# Release runbook

Work in progress: built up task by task in Phase 6B of `PLAN.md`. This version covers Android signing only.

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
