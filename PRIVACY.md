# Privacy

Nascope does not collect, store on a server or share any data about you or your devices.

- **No accounts, no analytics, no telemetry, no advertising, no crash reporting, no third-party SDKs.** The app never contacts the developer or any service other than the servers you add.
- **Network:** the app connects only to the Glances servers you enter (host, port, optional user name and password) to read their monitoring data. The Android app requests only the `INTERNET` permission.
- **What is stored on your device:**
  - the list of servers you added (alias, host, port, protocol, user name) and your preferences (refresh interval, demo mode), in the app's private storage;
  - the passwords of servers that use authentication, in the Android Keystore (encrypted) or the operating system's keyring on desktop. If no keyring exists, a password is kept in memory for the session only and not written anywhere.
- **Backups:** on Android the server list may be included in your system backups, the stored passwords are excluded.
- **Logs:** release builds log warnings and errors only, never server addresses, passwords or monitoring data.
- **Removal:** deleting a server in the app removes its entry and its stored password; uninstalling the app removes everything it stored.

If this changes in a future version, this file will say so before the change ships.
