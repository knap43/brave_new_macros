# Macros

Small trigger → action automations for Android. Kotlin, Jetpack Compose, Material 3 — same dark,
amber-accented look as Journal, Files and Reader.

Triggers: **WiFi connects** to a chosen network, or **a time of day** on chosen weekdays.
Actions: **Bluetooth** on or off.

## Build

```fish
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The GitHub workflow in `.github/workflows/build.yml` builds debug and release APKs if this goes into a repo.

## How it works

- **Watching WiFi.** A `PendingIntent` network request — no background service, no persistent
  notification. Android wakes `NetworkReceiver` when a WiFi network comes up; the watch is re-armed
  after reboot and app updates.
- **Time.** One exact alarm per time macro, re-set after each run, after reboot, and when the clock
  or time zone changes. `USE_EXACT_ALARM` makes it fire on the minute without a settings detour.
- **Matching.** Reading the SSID needs precise location *and* “Allow all the time”, with location
  services on. The home screen lists whatever is still missing.
- **Switching Bluetooth.** Since Android 13 ordinary apps can't. In order of preference:
  1. direct API (Android 12 and older),
  2. Shizuku — `cmd bluetooth_manager disable`, silent,
  3. a notification that opens Bluetooth settings for a one-tap finish.
- **Activity** (clock icon) logs every run and why it did what it did.

## Shizuku on realme UI

Shizuku stops at every reboot. Restart it from the Shizuku app via Wireless debugging
(Developer options), then grant Macros access from the home screen card. Also set Macros' battery
usage to *Unrestricted*, or realme UI may freeze it and swallow the WiFi wake-up.

## Adding macro kinds

Triggers and actions are sealed types in `data/Model.kt`. Add a subclass and the compiler flags every
exhaustive `when` that needs a branch — description, editor row, engine.
