# Nexa Camera — HONOR 90 Camera Lab

Phase 1 + Phase 2 only. This is a command-line-buildable Android Camera2 diagnostic app. It inspects the capabilities exposed by the physical device; it does not assume that advertised features such as 200MP, RAW, LOG, or 4K60 are available through public APIs.

## Build

Install JDK 17, Android SDK command-line tools, SDK Platform 35, Build Tools 35.0.0, NDK 27.2.12479018, and CMake 3.30.5. Run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). If Gradle is not installed, the wrapper bootstraps Gradle 8.10.2 into the ignored `.gradle-dist` directory. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Open the app on the HONOR 90, grant camera access, tap Rescan, and tap Export JSON. The report is saved to Downloads.

## CI

GitHub Actions runs the same build and test path and uploads `app-debug.apk` as an artifact. No Android Studio is required.

## Limitations

Classification is evidence-based but conservative; camera IDs and vendor-specific behavior must be confirmed from the exported report. Phase 3 (preview/capture engine) intentionally has not started.
