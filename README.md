# FireLauncher 🔥

A high-performance custom home-screen launcher designed specifically for **Amazon Fire TV (Fire OS)**, built in Kotlin using **Jetpack Compose for TV (tv-material)**, **MVVM architecture**, **Preferences DataStore**, and **AndroidX Palette** for dynamic ambient theming.

<p align="center">
  <img src="docs/firelauncher_screenshot.jpg" alt="FireLauncher Interface Preview - Dynamic r/TVWallpapersSFW Background, Live Weather, and App Options Context Menu" width="100%" style="border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5);" />
  <br/>
  <em>FireLauncher on Fire TV: Dynamic 4K wallpaper, live Open-Meteo weather, A–Z / Recent / Frequent sorting, and D-pad App Options menu (Force Stop / App Info & Favorites)</em>
</p>

---

## 📺 Key Features

1. **Dual Leanback & Standard App Scanner**:
   - Queries both `android.intent.category.LEANBACK_LAUNCHER` and `android.intent.category.LAUNCHER`.
   - Android 11+ (API 30+) `<queries>` visibility support.
   - De-duplicates by package name (preferring Leanback entries for 16:9 TV banners).
   - Excludes self and sorts alphabetically A–Z.
   - Launches apps with explicit `ComponentName` intents and `FLAG_ACTIVITY_NEW_TASK`.
2. **Dynamic Ambient Background Gradients**:
   - Uses AndroidX `Palette` to extract vibrant accent colors from app banners or icons.
   - Clamps HSV saturation (`0.55`–`0.95`) and value/brightness (`0.50`–`0.88`) to guarantee vivid, legible colors that never become washed-out or near-black.
   - Animated background gradient dynamically transitions to the focused card's accent color.
3. **Custom Fire TV D-Pad Focus Behavior**:
   - 16:9 rounded cards built on `androidx.tv.material3.Surface`.
   - On D-pad focus: card scales to **1.12x**, gains a crisp **2dp white border**, and emits an **accent-color tinted glow**.
   - Focus defaults to the first card on application launch.
4. **Favorites Management via DataStore**:
   - Long-press OK (D-pad center) on any card to toggle favorite status.
   - Saved persistently in AndroidX Preferences DataStore.
   - Favorited cards display a distinctive yellow dot in the upper corner.
   - Dedicated "Favorites" row dynamically appears whenever non-empty.
5. **System Shortcuts Row**:
   - Instant 1-click access to TV system settings: **Wi-Fi**, **Display**, **Sound**, **Bluetooth**, and **All Settings**.
6. **Dynamic r/TVWallpapersSFW Homepage Wallpapers & Vivid Glass Scrim**:
   - Fetches authentic 16:9 high-resolution landscape TV wallpapers directly from `r/TVWallpapersSFW`.
   - Rotates automatically every **30 minutes** with smooth Coil image crossfading.
   - **Enhanced Wallpaper Visibility**: Replaced heavy opaque masks with an ultra-clear, translucent glass scrim (12%–52% gradient). Wallpapers are now bright, vivid, and cinematic across the entire screen while maintaining 100% text/card legibility with subtle drop shadows.
   - Includes a discreet top-right attribution chip showing the wallpaper title and source subreddit.
   - Top bar "Next Wallpaper" button (or `W` key in simulator) allows manual instant cycling.
   - Built-in curated 4K fallback wallpapers ensure seamless operation even offline or in network-restricted environments.
7. **Library 'Sort By' Menu (A–Z, Most Recent, Most Frequent)**:
   - Dynamic sorting modal dialog accessible from the Top Bar ("Sort" button / `S` key) and directly on the "All Apps" row header (`Sorted by: ... ▾`).
   - Three distinct sort orders:
     - **Alphabetical (A–Z)**: Classic alphabetical ordering by application title.
     - **Most Recently Used (MRU)**: Orders apps by latest launch timestamp, showing friendly relative time badges (e.g. *"5m ago"*, *"2h ago"*).
     - **Most Frequently Used (MFU)**: Orders apps by total launch frequency, showing play count badges (e.g. *"24×"*).
   - Tracks app launches and saves sorting preferences persistently in Preferences DataStore (`KEY_APP_SORT_ORDER` and `KEY_APP_USAGE_STATS`).
8. **Accurate Live Weather with Global City Search (Open-Meteo API)**:
   - Replaces mock weather with real-time meteorological forecasts powered by **Open-Meteo** (free, no API key required, WMO weather standards).
   - Shows live temperature, weather condition icon/summary, high/low, humidity %, and wind speed.
   - **Global City Search Modal**: Users can search for any city in the world using Open-Meteo Geocoding, view region/country, and add it with one click.
   - Custom cities are saved persistently in AndroidX Preferences DataStore.
   - Quick toggle between **Fahrenheit (°F)** and **Celsius (°C)**.
9. **Top Bar, Global App Search & Live Clock**:
   - Dedicated **Global Search** button with real-time app filtering by name and package identifier, expandable search bar, and empty search state.
   - Live clock honoring 12-hour / 24-hour system preferences, plus formatted date.
   - Hero header showing the currently focused app's title, category, and contextual action hint (*"Hold OK to add to favorites"* / *"Hold OK to remove from favorites"*).
10. **TV Overscan & Remote Navigation Safeguards**:
    - 48dp horizontal margins for TV overscan-safe rendering on any HDMI display.
    - Hardware Back button is deliberately absorbed and ignored to prevent accidental launcher exit, but gracefully dismisses active search or modal dialogs first.
    - Pressing the Home key while the launcher is open delivers an intent to `singleTask`'s `onNewIntent`, automatically scrolling to the top and refocusing the primary card.
    - Automatic app catalog refresh in `onResume()`.

---

## 🛠️ Project Setup & Specifications

- **Module**: Single `:app` module
- **Gradle**: Kotlin DSL (`.kts`) with Gradle 8.9 wrapper
- **Android Gradle Plugin (AGP)**: `8.5.2`
- **Kotlin**: `2.0.20` with `org.jetbrains.kotlin.plugin.compose` plugin
- **Compile SDK**: `34`
- **Target SDK**: `34`
- **Min SDK**: `25` (Fire OS 6 / Android 7.1+)
- **Orientation**: Locked to Landscape
- **Compose BOM**: `2024.09.03`
- **TV Material**: `androidx.tv:tv-material:1.0.0`
- **DataStore**: `androidx.datastore:datastore-preferences:1.1.1`
- **Palette**: `androidx.palette:palette-ktx:1.0.0`
- **Coil**: `io.coil-kt:coil-compose:2.7.0`
- **Offline**: 100% offline, requires no network access and no API keys.

---

## 🚀 How to Build the Project

### Prerequisites
- JDK 17 (recommended: Azul Zulu 17 or Temurin 17)
- Android SDK with Platform 34 and Build-Tools 34.0.0 installed
- Android Studio Ladybug (2024.2+) or command-line Gradle

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
./gradlew assembleRelease
```

---

## 📡 Installing on Amazon Fire TV via ADB

### Step 1: Enable Developer Options & ADB on Fire TV
1. On your Fire TV, navigate to **Settings** > **My Fire TV** (or **Device & Software**) > **About**.
2. Highlight your device's name and press the **Select / Center button 7 times** until you see the notification *"No need, you are already a developer."*
3. Press **Back**, select **Developer Options**.
4. Turn **ADB Debugging** to **ON**.
5. Note your Fire TV's IP address by going to **Settings** > **My Fire TV** > **About** > **Network** (e.g., `192.168.1.50`).

### Step 2: Connect over Wi-Fi & Install APK
Open a terminal on your computer and execute:

```bash
# Connect to Fire TV over Wi-Fi on port 5555
adb connect <ip>:5555

# Example:
# adb connect 192.168.1.50:5555
```
*(Accept the "Allow USB/Network debugging?" prompt on your Fire TV screen if it appears).*

Install the debug APK:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To launch FireLauncher immediately:
```bash
adb shell am start -n com.example.firelauncher/.MainActivity
```

---

## ⚠️ Important Note Regarding Fire OS Default Launcher Settings

> **Fire OS Limitation**: Amazon Fire OS does **not** provide a native GUI setting or standard Android menu option to change or select the default home launcher.
>
> On standard Android TV / Google TV, pressing Home or installing a second launcher prompts the system selector. On Amazon Fire OS, the stock Amazon launcher is hardcoded into `AmazonFireLauncher` / `com.amazon.tv.launcher`.

### Options to Use FireLauncher as Your Primary Screen:
1. **Remote Button Remapping (Recommended & Safest)**:
   - Install **Button Mapper** or **Launcher Manager** (via Downloader or ADB).
   - Remap the Home button single-press or double-press to launch `com.example.firelauncher/.MainActivity`.
2. **Launch on Boot (Accessibility Service / Boot Receiver)**:
   - Configure a boot launcher utility to automatically start FireLauncher when the Fire TV finishes starting up.
3. **Direct Quick-Launch**:
   - Add FireLauncher to your favorite quick-access app bar on the default Fire TV home screen for 1-click entry into an ad-free, clutter-free leanback interface.
4. **ADB Package Disabling (Advanced Users Only)**:
   - On older Fire OS versions (Fire OS 6 and early Fire OS 7), power users disabled the stock package via:
     ```bash
     adb shell pm disable-user --user 0 com.amazon.tv.launcher
     ```
     *(Note: Amazon has blocked disabling the stock launcher package on newer Fire OS 7 and 8 updates, which makes Button Remapping the preferred modern approach).*

---

## 🛡️ CleanYourTV Integration: Safe Non-Root Debloating

Inspired by [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), FireLauncher incorporates safe debloating profiles specifically tailored for Amazon Fire OS. By disabling intrusive sponsored ad services and continuous diagnostic telemetry daemons, you recover **~275MB of RAM**, eliminate video autoplay stutters, and ensure snappy D-pad navigation.

### Quick Debloat Commands (100% Safe & Reversible)
```bash
# Disable Amazon sponsored home screen banner & video ad engine
adb shell pm disable-user --user 0 com.amazon.advertising.firetv

# Disable commercial promotional broadcaster & rental deal checks
adb shell pm disable-user --user 0 com.amazon.device.sale.service

# Disable telemetry, metrics logging, and clickstream analytics
adb shell pm disable-user --user 0 com.amazon.client.metrics

# Disable crash reporter daemon
adb shell pm disable-user --user 0 com.amazon.device.crashmanager

# Disable sponsored video ad screensaver
adb shell pm disable-user --user 0 com.amazon.ftv.screensaver
```

### Factory Revert Command
To re-enable all stock packages at any time:
```bash
adb shell pm enable com.amazon.advertising.firetv
adb shell pm enable com.amazon.device.sale.service
adb shell pm enable com.amazon.client.metrics
adb shell pm enable com.amazon.device.crashmanager
adb shell pm enable com.amazon.ftv.screensaver
```

---

## 📂 Project Structure

```
FireLauncher/
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Project name & repository configuration
├── gradle.properties                  # JVM & AndroidX optimization flags
├── gradle/wrapper/
│   └── gradle-wrapper.properties      # Gradle 8.9 distribution URL
├── app/
│   ├── build.gradle.kts               # App module dependencies & SDK 34 config
│   ├── proguard-rules.pro             # Proguard keep rules
│   └── src/main/
│       ├── AndroidManifest.xml        # Leanback & Home intent filters + package queries
│       ├── res/
│       │   ├── drawable/
│       │   │   ├── banner.xml         # 320x180 vector Leanback banner
│       │   │   ├── ic_launcher_background.xml
│       │   │   └── ic_launcher_foreground.xml
│       │   ├── mipmap-anydpi-v26/
│       │   │   ├── ic_launcher.xml    # Adaptive launcher icon
│       │   │   └── ic_launcher_round.xml
│       │   ├── values/
│       │   │   ├── colors.xml         # Color definitions
│       │   │   ├── strings.xml        # Localization strings
│       │   │   └── themes.xml         # Fullscreen TV theme
│       │   └── xml/
│       │       └── network_security_config.xml # TLS-only policy & cleartext traffic block
│       └── java/com/example/firelauncher/
│           ├── FireLauncherApp.kt     # Application class
│           ├── MainActivity.kt        # SingleTask TV activity + Home & Back interceptors
│           ├── data/
│           │   ├── AppModel.kt            # App data entity with launch stats
│           │   ├── AppRepository.kt       # Dual scanner (Leanback + Launcher) & Explicit launcher
│           │   ├── AppSortOrder.kt        # AppSortOrder enum (A-Z, Recent, Frequent) & AppUsageStat
│           │   ├── FavoritesStore.kt      # Preferences DataStore store for favorites, sort order & usage stats
│           │   ├── PaletteHelper.kt       # Palette extractor with HSV saturation/brightness clamping
│           │   ├── WallpaperModel.kt      # r/TVWallpapersSFW data entity
│           │   ├── WallpaperRepository.kt # Subreddit fetcher, 30-min auto-rotation & curated fallbacks
│           │   ├── WeatherData.kt         # Open-Meteo live weather & city location models
│           │   └── WeatherRepository.kt   # Open-Meteo live forecast & global geocoding city search
│           └── ui/
│               ├── LauncherViewModel.kt   # MVVM StateFlow coordinator (apps, search, weather, wallpaper)
│               ├── LauncherRoot.kt        # State collector, 48dp overscan padding, wallpaper & ambient vignette
│               ├── components/
│               │   ├── AppCard.kt         # 16:9 Surface, 1.12x scale, white border, glow, yellow dot
│               │   ├── AppContextMenuDialog.kt # Long-press context menu (Favorites, Force Stop/App Info, Launch)
│               │   ├── AppRow.kt          # TvLazyRow with category header
│               │   ├── HeroSection.kt     # App title, category & hold-OK instructions
│               │   └── TopBar.kt          # Live weather with city search modal, wallpaper cycle, clock & search
│               └── theme/
│                   ├── Color.kt         # Color palette constants
│                   └── Theme.kt         # Material3 TV DarkColorScheme
├── docs/
│   └── firelauncher_screenshot.jpg    # 16:9 UI interface preview screenshot
├── tools/
│   ├── get_tv_ip.py                   # Fire TV IP discovery, IPv4 regex validator & SUCCESS token CLI
│   └── clear_launcher_cache.sh        # ADB shell utility to wipe launcher cache & free device RAM
├── AI_STEPS.md                        # AI agent automation manifest (ADB connect, APK install, debloat, reboot)
└── README.md
```

---

## ⚡ Performance Tuning & Low-End Fire TV Optimizations

FireLauncher is engineered specifically for resource-constrained Fire TV Sticks (1GB–1.5GB RAM, 8GB flash storage):

1. **Compact Storage & Cache Cap (`FireLauncherApp.kt`)**:
   - Implements Coil `ImageLoaderFactory` with a strict memory cache ceiling capped at **10% of available application RAM**.
   - Implements a hard disk cache ceiling capped at exactly **15MB** (`DiskCache.Builder().maxSizeBytes(15L * 1024 * 1024)`), preventing automated `r/TVWallpapersSFW` wallpaper downloads from exhausting device storage over time.
2. **ProGuard & Code Shrinking (`app/build.gradle.kts`)**:
   - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `buildTypes.release` with `proguard-android-optimize.txt` to minimize compiled APK footprint.
3. **"Force Stop / App Info" Context Menu (`AppContextMenuDialog.kt`)**:
   - Extending the Long-Press D-pad OK gesture on any app card opens a leanback context menu.
   - Provides direct 1-click access to the Fire OS system settings management window via `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` with `Uri.parse("package:${app.packageName}")` for immediate Force Stop and app cache clearing.
4. **Memory Recovery Script (`tools/clear_launcher_cache.sh`)**:
   - Lightweight shell utility to purge the launcher's temporary data and image cache over ADB (`adb shell pm clear com.example.firelauncher`) and cleanly relaunch the interface without rebooting.

---

## 🔒 Security & Hardening Safeguards

FireLauncher adheres to OWASP Mobile Application Security Verification Standards (MASVS v2.0):

1. **Backup Data Protection**:
   - `android:allowBackup="false"` in `AndroidManifest.xml` prevents arbitrary data extraction and state tampering over ADB debugging (`adb backup`).
2. **Network Security Configuration**:
   - Enforces TLS-only communication (`cleartextTrafficPermitted="false"`) via `res/xml/network_security_config.xml` and `android:usesCleartextTraffic="false"`, preventing Man-in-the-Middle (MITM) attacks.
3. **Delimiter Injection Defense**:
   - DataStore persistence sanitizes pipe characters (`.replace("|", "-")`) and checks numeric coordinate bounds (-90..90 lat, -180..180 lon) before serialization.
4. **Remote URL Scheme & Host Validation**:
   - Remote wallpapers fetched from Reddit are strictly validated for `https://` protocol and restricted to verified CDN hosts (`i.redd.it`, `preview.redd.it`, `i.imgur.com`, `images.unsplash.com`).
5. **Intent Hijacking Mitigation**:
   - Internal system shortcuts are explicitly scoped to `context.packageName` (`intent.setPackage(context.packageName)`) and validated with `resolveActivity()` before launching.
6. **Input Length Bounds**:
   - Weather and geocoding search queries are bounded to a maximum length of 64 characters to mitigate denial of service and URL overflow issues.
7. **Web Simulator Hardening**:
   - Configured with Content Security Policy (`CSP`), `strict-origin-when-cross-origin` referrer policy, and `rel="noopener noreferrer"` on external hyperlinks.

---

## 📄 License
Licensed under Apache License 2.0.
