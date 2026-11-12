# Salah Times — Native Android

A dark-premium, original Kotlin / Jetpack Compose prayer-times application.

## Requirements
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35
- Internet access for the first Gradle dependency sync (the app itself calculates offline)

## Open and run
1. Unzip this archive.
2. In Android Studio choose **Open** and select the `salah_times_project` directory.
3. Allow Android Studio to download/sync Gradle dependencies.
4. Run the `app` configuration on an Android 8+ device or emulator.
5. For APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

If your system has Gradle 8.9 installed, you can build with `gradle assembleDebug` from the project root. This ZIP deliberately does not contain a Gradle wrapper JAR; Android Studio can sync using its configured Gradle installation, or you can generate a wrapper with `gradle wrapper --gradle-version 8.9` (and then use `./gradlew assembleDebug`).

## Included
- Daily prayer times, next-prayer countdown, automatic DST handling via Java time
- Qibla bearing and sensor-driven compass (if rotation-vector sensor is present)
- Prayer alerts for Fajr, Dhuhr, Asr, Maghrib, Isha
- Settings: coordinates, device location, calculation method, Hanafi Asr, alerts
- Offline calculations, no account, no ads, no tracking

## Important limitations
- **Adhan:** v1 sends a notification with the system notification sound; no full recited Adhan audio is bundled.
- **Location:** defaults to Alexandria, Virginia as a sample and can be changed. Device location uses the most recent cached GPS/network fix, not an active location request.
- **Timezone:** uses the device timezone, not timezone lookup from coordinates. Change the device timezone when using remote coordinates.
- **Compass:** uses rotation-vector sensor heading and does not correct magnetic north to true north. Bearing text is true-north based; sensor pointer may have local declination error.
- **Prayer times:** approximate NOAA solar equations; no local mosque adjustments, altitude corrections, or advanced polar method. Confirm with your mosque.
- **Alarms:** on Android 12+ exact alarms require separate special access; fallback may be delayed. After reboot/timezone changes alarms are rescheduled. App must be opened once to set alerts.
- **Build:** source project prepared but not compiled or device-tested in this environment.

## Originality
Salah Times uses its own branding and UI; no Muslim Pro assets, code, or trademarks are used.
