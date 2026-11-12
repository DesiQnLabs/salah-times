# Salah Times 1.1 — Phone upgrade

This ZIP contains the **entire Android Studio project**, with the existing package name retained (`com.salahtimes.app`). It is **source code, not an APK**.

## Install update using Termux

1. Download this ZIP to the Android Downloads folder.
2. Open Termux and run:

```sh
cd ~/salah-work
unzip -o ~/storage/downloads/salah_times_v1_1.zip
# Ensure the workflow and app files are staged
git add .
git commit -m "Salah Times v1.1 calendar and themes"
git push origin main
```

If your downloaded ZIP has a different filename, use that name. The ZIP contains files at the repository root (no enclosing directory). `unzip -o` overwrites the matching project files but does not delete your Git history.

3. Open https://github.com/DesiQnLabs/salah-times/actions and download the successful run's APK artifact. Extract the artifact ZIP and install `app-debug.apk`.

## Changes

- Shadcn-inspired native Compose visual palette (not the React shadcn/ui library)
- Persistent light/dark setting
- Calendar with date picker and previous/today/next controls, using existing offline prayer calculator
- Feature emojis matching screenshot concepts (visual previews only for Duas/Tasbih/Inspiration/Journal)
- Retains existing Qibla, countdown and standard system notification alerts
- Version 1.1.0 / versionCode 2

## Notes

Prayer times are astronomical approximations based on configured coordinates, calculation method, and the **device's timezone**. The app is not a substitute for a verified local mosque timetable. This project has not been compiled in this environment; GitHub Actions will perform the build.
