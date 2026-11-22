# Build from source

[← Back to README](../README.md)

JDK 17 and Android SDK 34.

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

<details>
<summary>Releases & signing</summary>

- **CI** (`.github/workflows/android-ci.yml`) runs the unit tests, lint and both builds on every push and pull request to `main`.
- **Releases** (`.github/workflows/release.yml`): push a `vX.Y.Z` tag, or run the workflow by hand with a version. It builds the signed release APK, a debug APK and a SHA-256 checksum file, and publishes a GitHub Release. The version code is `MAJOR×10000 + MINOR×100 + PATCH`.
- **Signing**: add the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Without them, releases use a throwaway debug key, and updates then can't install over each other.
- **Forks**: set `UPDATE_REPO` in `app/build.gradle.kts` so the in-app updater checks your releases.

</details>

<details>
<summary>Project layout</summary>

```
app/src/main/java/com/knightmstr/firelauncher/
├── data/          apps, preferences, wallpapers, weather, Kids-mode PIN
├── home/          Home button watcher (accessibility service)
├── screensaver/   system screensaver + Fire TV screensaver/sleep settings
├── media/         Now playing (media sessions)
├── update/        GitHub update check, download, verify, install
└── ui/            Compose screens: home, dock, All Apps, dialogs, settings
tools/             get_tv_ip.py (finds the TV's IP, used by AI_STEPS.md)
                   clear_launcher_cache.sh (clears FireLauncher's image cache on low-memory sticks)
```

</details>
