import { ProjectFile } from '../types';

export const ANDROID_PROJECT_FILES: ProjectFile[] = [
  {
    path: 'settings.gradle.kts',
    name: 'settings.gradle.kts',
    language: 'kotlin',
    category: 'gradle',
    description: 'Root Gradle configuration defining Google/Maven repositories and :app module inclusion',
    content: `pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\\\.android.*")
                includeGroupByRegex("com\\\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FireLauncher"
include(":app")
`
  },
  {
    path: 'build.gradle.kts',
    name: 'build.gradle.kts',
    language: 'kotlin',
    category: 'gradle',
    description: 'Root build script declaring AGP 8.5.2, Kotlin 2.0.20 and Compose compiler plugin',
    content: `// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
`
  },
  {
    path: 'gradle.properties',
    name: 'gradle.properties',
    language: 'properties',
    category: 'gradle',
    description: 'Gradle JVM arguments, nonTransitiveRClass and AndroidX settings',
    content: `# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
org.gradle.configuration-cache=true
`
  },
  {
    path: 'gradle/wrapper/gradle-wrapper.properties',
    name: 'gradle-wrapper.properties',
    language: 'properties',
    category: 'gradle',
    description: 'Gradle 8.9 wrapper distribution config',
    content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
`
  },
  {
    path: 'app/build.gradle.kts',
    name: 'app/build.gradle.kts',
    language: 'kotlin',
    category: 'gradle',
    description: 'App module build script: SDK 34, minSdk 25, tv-material 1.0.0, DataStore, Palette, Compose BOM',
    content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.firelauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.firelauncher"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ""
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core & Activity Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose BOM & Foundations
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")

    // TV Material 3 for Leanback / Fire TV
    implementation("androidx.tv:tv-material:1.0.0")

    // Lifecycle Compose (ViewModel + StateFlow collection)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // AndroidX Palette for dynamic accent colors
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Coil for Compose image loading (App icons / banners)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Debugging tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
`
  },
  {
    path: 'app/proguard-rules.pro',
    name: 'proguard-rules.pro',
    language: 'pro',
    category: 'gradle',
    description: 'ProGuard keep rules for FireLauncher and TV Material',
    content: `# ProGuard rules for FireLauncher
-keep class com.example.firelauncher.** { *; }
-dontwarn androidx.tv.**
`
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    language: 'xml',
    category: 'manifest',
    description: 'Leanback & Home intent filters, package visibility <queries>, singleTask, landscape',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Leanback TV & Touchscreen flags -->
    <uses-feature
        android:name="android.software.leanback"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />

    <!-- Package visibility queries for Android 11+ (API 30+) to scan TV & standard launchers -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:name=".FireLauncherApp"
        android:allowBackup="true"
        android:banner="@drawable/banner"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.FireLauncher">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:stateNotNeeded="true"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode">

            <!-- Leanback launcher intent filter for Android TV / Fire TV -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>

            <!-- Standard home launcher intent filter -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
    </application>

</manifest>
`
  },
  {
    path: 'app/src/main/res/drawable/banner.xml',
    name: 'banner.xml',
    language: 'xml',
    category: 'res',
    description: '320x180 vector Leanback TV banner with flame emblem and gradient',
    content: `<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="320dp"
    android:height="180dp"
    android:viewportWidth="320"
    android:viewportHeight="180">

    <!-- Background card -->
    <path
        android:pathData="M0,0h320v180h-320z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="320"
                android:endY="180"
                android:type="linear">
                <item android:offset="0.0" android:color="#FF1E1B2E" />
                <item android:offset="0.5" android:color="#FF151221" />
                <item android:offset="1.0" android:color="#FF0D0B14" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Flame glow background -->
    <path
        android:pathData="M160,90m-70,0a70,70 0,1 1,140 0a70,70 0,1 1,-140 0">
        <aapt:attr name="android:fillColor">
            <gradient
                android:centerX="160"
                android:centerY="90"
                android:gradientRadius="70"
                android:type="radial">
                <item android:offset="0.0" android:color="#4DFF6A00" />
                <item android:offset="1.0" android:color="#00FF6A00" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Fire emblem -->
    <path
        android:pathData="M160,45 C155,62 142,75 142,95 C142,118 152,130 160,132 C168,130 178,118 178,95 C178,80 171,68 168,60 C167,70 163,78 158,82 C161,72 163,55 160,45 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="160"
                android:startY="45"
                android:endX="160"
                android:endY="132"
                android:type="linear">
                <item android:offset="0.0" android:color="#FFFFB703" />
                <item android:offset="0.6" android:color="#FFFB8500" />
                <item android:offset="1.0" android:color="#FFD00000" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Inner hot flame core -->
    <path
        android:pathData="M160,88 C156,98 152,106 152,116 C152,126 156,130 160,131 C164,130 168,126 168,116 C168,108 164,101 160,88 Z"
        android:fillColor="#FFFFF3B0" />

    <!-- Sleek border outline -->
    <path
        android:pathData="M1,1h318v178h-318z"
        android:strokeWidth="1.5"
        android:strokeColor="#26FFFFFF" />
</vector>
`
  },
  {
    path: 'app/src/main/res/values/strings.xml',
    name: 'strings.xml',
    language: 'xml',
    category: 'res',
    description: 'String resources for rows, hero hints, and system shortcuts',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FireLauncher</string>
    <string name="row_favorites">Favorites</string>
    <string name="row_all_apps">All Apps</string>
    <string name="row_system">System</string>
    <string name="hint_hold_ok">Hold OK to add to favorites</string>
    <string name="hint_hold_ok_remove">Hold OK to remove from favorites</string>
    <string name="shortcut_wifi">Wi-Fi</string>
    <string name="shortcut_display">Display</string>
    <string name="shortcut_sound">Sound</string>
    <string name="shortcut_bluetooth">Bluetooth</string>
    <string name="shortcut_settings">All Settings</string>
</resources>
`
  },
  {
    path: 'app/src/main/res/values/colors.xml',
    name: 'colors.xml',
    language: 'xml',
    category: 'res',
    description: 'XML color definitions matching dark TV theme and Fire accents',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="launcher_background">#0F1117</color>
    <color name="launcher_surface">#1A1D27</color>
    <color name="primary_accent">#FF9900</color>
    <color name="favorite_yellow">#FFD600</color>
    <color name="text_primary">#F3F4F6</color>
    <color name="text_secondary">#9CA3AF</color>
</resources>
`
  },
  {
    path: 'app/src/main/res/values/themes.xml',
    name: 'themes.xml',
    language: 'xml',
    category: 'res',
    description: 'Fullscreen dark TV theme without action bar',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FireLauncher" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/launcher_background</item>
        <item name="android:statusBarColor">@color/launcher_background</item>
        <item name="android:navigationBarColor">@color/launcher_background</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
    </style>
</resources>
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/AppModel.kt',
    name: 'AppModel.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Data model representing installed Leanback/standard apps and system shortcuts',
    content: `package com.example.firelauncher.data

import android.graphics.drawable.Drawable

/**
 * Model representing an installed application or a system shortcut on Fire TV.
 */
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
    val banner: Drawable? = null,
    val accentColor: Int = 0xFFFF9900.toInt(),
    val isSystemShortcut: Boolean = false,
    val systemIntentAction: String? = null
) {
    val uniqueKey: String
        get() = if (isSystemShortcut) "sys:\${systemIntentAction ?: label}" else packageName
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/PaletteHelper.kt',
    name: 'PaletteHelper.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Extracts dominant accent color and clamps HSV saturation/brightness to avoid washed-out or black colors',
    content: `package com.example.firelauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette

/**
 * Utility for extracting and clamping accent colors from app banners or icons.
 */
object PaletteHelper {

    private const val DEFAULT_ACCENT_COLOR = 0xFFFF9900.toInt() // Fire OS warm orange

    /**
     * Extracts a dominant or vibrant accent color from the provided drawable,
     * and clamps HSV saturation and brightness so it is never washed-out or near-black.
     */
    fun extractClampedAccentColor(drawable: Drawable?): Int {
        if (drawable == null) return DEFAULT_ACCENT_COLOR
        val bitmap = drawableToBitmap(drawable) ?: return DEFAULT_ACCENT_COLOR

        return try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()

            val rawColor = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: DEFAULT_ACCENT_COLOR

            clampColor(rawColor)
        } catch (_: Exception) {
            DEFAULT_ACCENT_COLOR
        }
    }

    /**
     * Clamps the HSV saturation and value (brightness) so the color remains vibrant,
     * vivid, and clearly visible on dark TV backgrounds without turning washed-out or near-black.
     */
    fun clampColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Saturation clamped between 0.55 and 0.95 (prevents dull washed-out pastel/white/grey)
        hsv[1] = hsv[1].coerceIn(0.55f, 0.95f)

        // Value (brightness) clamped between 0.50 and 0.88 (prevents dark near-black or blinding white)
        hsv[2] = hsv[2].coerceIn(0.50f, 0.88f)

        return Color.HSVToColor(hsv)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(160) else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(90) else 96

        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/CleanTvManager.kt',
    name: 'CleanTvManager.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'CleanYourTV integration: Safe non-root Fire OS debloat lists, ADB script generator, and RAM recovery metrics',
    content: `package com.example.firelauncher.data

/**
 * CleanTV Manager: Inspired by Gathin23/cleanyourtv.
 * Provides safe, non-root debloating profiles and telemetry-disabling commands
 * for Amazon Fire TV, freeing up device RAM, eliminating autoplay home ads,
 * and reducing background D-pad input lag.
 */
object CleanTvManager {

    data class BloatPackage(
        val packageName: String,
        val displayName: String,
        val description: String,
        val category: String, // "Ads", "Telemetry", "Autoplay", "Background Services"
        val ramSavingMb: Int = 45,
        val isSafeToDisable: Boolean = true
    )

    val SAFE_DEBLOAT_PACKAGES = listOf(
        BloatPackage(
            packageName = "com.amazon.advertising.firetv",
            displayName = "Amazon Sponsored Ads Engine",
            description = "Injects banner video ads and sponsored recommendations on the home screen",
            category = "Ads",
            ramSavingMb = 65
        ),
        BloatPackage(
            packageName = "com.amazon.device.sale.service",
            displayName = "Promotions & Sale Broadcaster",
            description = "Runs background checks for shopping events and Prime Video rental deals",
            category = "Ads",
            ramSavingMb = 35
        ),
        BloatPackage(
            packageName = "com.amazon.client.metrics",
            displayName = "Amazon Telemetry & Metrics",
            description = "Logs app usage patterns, clickstreams, and sends telemetry data to Amazon servers",
            category = "Telemetry",
            ramSavingMb = 50
        ),
        BloatPackage(
            packageName = "com.amazon.device.crashmanager",
            displayName = "Crash Reporter Daemon",
            description = "Continually monitors system logs and dumps stack traces over the network",
            category = "Telemetry",
            ramSavingMb = 30
        ),
        BloatPackage(
            packageName = "com.amazon.ftv.screensaver",
            displayName = "Sponsored Ambient Screensaver",
            description = "Replaces ambient photos with commercial ads when TV is left idle",
            category = "Autoplay",
            ramSavingMb = 55
        ),
        BloatPackage(
            packageName = "com.amazon.bueller.music",
            displayName = "Amazon Music Background Preloader",
            description = "Pre-caches promotional audio tracks even if you use Spotify or YouTube",
            category = "Background Services",
            ramSavingMb = 40
        )
    )

    val totalRamFreedEstimateMb: Int
        get() = SAFE_DEBLOAT_PACKAGES.sumOf { it.ramSavingMb }

    fun generateDebloatScript(): String {
        val sb = StringBuilder()
        sb.append("# CleanYourTV — Safe Fire TV Debloat Script\\n")
        sb.append("# Disables telemetry, sponsored ad engines, and background pre-loaders\\n\\n")
        for (pkg in SAFE_DEBLOAT_PACKAGES) {
            sb.append("adb shell pm disable-user --user 0 \${pkg.packageName}\\n")
        }
        sb.append("\\necho \\"[CleanYourTV] Done! Freed ~\${totalRamFreedEstimateMb}MB RAM and blocked home-screen ads.\\"\\n")
        return sb.toString()
    }

    fun generateRestoreScript(): String {
        val sb = StringBuilder()
        sb.append("# CleanYourTV — Reversible Factory Restore Script\\n")
        sb.append("# Re-enables all stock services if needed\\n\\n")
        for (pkg in SAFE_DEBLOAT_PACKAGES) {
            sb.append("adb shell pm enable \${pkg.packageName}\\n")
        }
        sb.append("\\necho \\"[CleanYourTV] Restored all stock packages successfully.\\"\\n")
        return sb.toString()
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/WeatherData.kt',
    name: 'WeatherData.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Weather data model with city presets, temperatures, conditions, and unit formatting',
    content: `package com.example.firelauncher.data

/**
 * Data model representing current weather conditions for the top-bar launcher widget.
 */
data class WeatherData(
    val city: String = "Seattle",
    val temperatureF: Int = 68,
    val temperatureC: Int = 20,
    val condition: String = "Partly Cloudy",
    val iconType: String = "partly_cloudy", // "clear", "partly_cloudy", "cloudy", "rain", "snow", "thunder"
    val highF: Int = 72,
    val lowF: Int = 55,
    val isCelsius: Boolean = false
) {
    val displayTemperature: String
        get() = if (isCelsius) "$temperatureC°C" else "$temperatureF°F"

    val displayHighLow: String
        get() = if (isCelsius) {
            "H:\${((highF - 32) * 5 / 9)}° L:\${((lowF - 32) * 5 / 9)}°"
        } else {
            "H:$highF° L:$lowF°"
        }

    companion object {
        val CITY_PRESETS = listOf(
            WeatherData("Seattle", 64, 18, "Partly Cloudy", "partly_cloudy", 68, 52),
            WeatherData("New York", 72, 22, "Sunny", "clear", 76, 61),
            WeatherData("Los Angeles", 78, 26, "Clear", "clear", 82, 65),
            WeatherData("Chicago", 61, 16, "Breezy", "cloudy", 65, 48),
            WeatherData("London", 59, 15, "Light Rain", "rain", 63, 50),
            WeatherData("Tokyo", 66, 19, "Clear", "clear", 70, 54),
            WeatherData("Paris", 63, 17, "Partly Cloudy", "partly_cloudy", 68, 52),
            WeatherData("Berlin", 57, 14, "Overcast", "cloudy", 61, 46),
            WeatherData("Sydney", 74, 23, "Sunny", "clear", 79, 62),
            WeatherData("Miami", 84, 29, "Scattered Showers", "rain", 88, 76),
            WeatherData("Denver", 65, 18, "Sunny", "clear", 71, 45)
        )

        fun forCity(cityName: String, isCelsius: Boolean = false): WeatherData {
            val preset = CITY_PRESETS.find { it.city.equals(cityName, ignoreCase = true) }
                ?: WeatherData(city = cityName, temperatureF = 70, temperatureC = 21, condition = "Fair", iconType = "clear", highF = 75, lowF = 58)
            return preset.copy(isCelsius = isCelsius)
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/FavoritesStore.kt',
    name: 'FavoritesStore.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Preferences DataStore manager for persistent favorites and weather city selection',
    content: `package com.example.firelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firelauncher_prefs")

/**
 * Manages persisting favorite app package names and selected weather city using AndroidX DataStore Preferences.
 */
class FavoritesStore(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages_set")
        private val KEY_WEATHER_CITY = androidx.datastore.preferences.core.stringPreferencesKey("weather_selected_city")
        private val KEY_WEATHER_CELSIUS = androidx.datastore.preferences.core.booleanPreferencesKey("weather_is_celsius")
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CITY] ?: "Seattle" }

    val isCelsiusFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CELSIUS] ?: false }

    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEATHER_CITY] = city
        }
    }

    suspend fun toggleTemperatureUnit() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_WEATHER_CELSIUS] ?: false
            preferences[KEY_WEATHER_CELSIUS] = !current
        }
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FAVORITES] ?: emptySet()
        }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(packageName)) {
                currentFavorites.remove(packageName)
            } else {
                currentFavorites.add(packageName)
            }
            preferences[KEY_FAVORITES] = currentFavorites
        }
    }

    suspend fun setFavorites(packageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FAVORITES] = packageNames
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/AppRepository.kt',
    name: 'AppRepository.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Scans LEANBACK_LAUNCHER + LAUNCHER apps, deduplicates, excludes self, loads banners, builds system shortcuts',
    content: `package com.example.firelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for scanning installed applications and system shortcuts,
 * sorting them, loading icons/banners, and launching them with explicit intents.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Scans installed TV and standard apps:
     * - Queries both LEANBACK_LAUNCHER and LAUNCHER
     * - De-duplicates by package name (prioritizing Leanback entries with TV banners)
     * - Excludes the launcher itself
     * - Loads label, icon, banner
     * - Extracts clamped accent color
     * - Sorts A-Z by label
     */
    suspend fun getInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val leanbackActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
            leanbackIntent,
            PackageManager.MATCH_ALL
        )
        val standardActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )

        val selfPackage = context.packageName

        // Maintain a map to deduplicate by packageName; leanback activities take precedence
        val appMap = LinkedHashMap<String, ResolveInfo>()

        // Add Leanback activities first
        for (info in leanbackActivities) {
            val pkg = info.activityInfo.packageName
            if (pkg != selfPackage) {
                appMap[pkg] = info
            }
        }

        // Add standard launcher activities if not already present
        for (info in standardActivities) {
            val pkg = info.activityInfo.packageName
            if (pkg != selfPackage && !appMap.containsKey(pkg)) {
                appMap[pkg] = info
            }
        }

        val appList = mutableListOf<AppModel>()

        for ((_, resolveInfo) in appMap) {
            val pkg = resolveInfo.activityInfo.packageName
            val activityName = resolveInfo.activityInfo.name
            val label = try {
                resolveInfo.loadLabel(packageManager).toString().trim()
            } catch (_: Exception) {
                resolveInfo.activityInfo.name
            }

            val icon: Drawable = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                packageManager.defaultActivityIcon
            }

            val banner: Drawable? = try {
                resolveInfo.activityInfo.loadBanner(packageManager)
                    ?: resolveInfo.activityInfo.applicationInfo.loadBanner(packageManager)
            } catch (_: Exception) {
                null
            }

            val accentColor = PaletteHelper.extractClampedAccentColor(banner ?: icon)

            appList.add(
                AppModel(
                    packageName = pkg,
                    activityName = activityName,
                    label = if (label.isNotEmpty()) label else pkg,
                    icon = icon,
                    banner = banner,
                    accentColor = accentColor,
                    isSystemShortcut = false
                )
            )
        }

        // Sort alphabetically case-insensitive
        appList.sortBy { it.label.lowercase() }
        appList
    }

    /**
     * Builds standard system shortcuts: Wi-Fi, Display, Sound, Bluetooth, All Settings.
     */
    fun getSystemShortcuts(): List<AppModel> {
        val dummyDrawable: Drawable = ColorDrawable(Color.TRANSPARENT)

        return listOf(
            AppModel(
                packageName = "system.wifi",
                activityName = "Settings.Wifi",
                label = "Wi-Fi",
                icon = dummyDrawable,
                accentColor = 0xFF00B4D8.toInt(), // Sky cyan
                isSystemShortcut = true,
                systemIntentAction = Settings.ACTION_WIFI_SETTINGS
            ),
            AppModel(
                packageName = "system.display",
                activityName = "Settings.Display",
                label = "Display",
                icon = dummyDrawable,
                accentColor = 0xFF7209B7.toInt(), // Deep violet
                isSystemShortcut = true,
                systemIntentAction = Settings.ACTION_DISPLAY_SETTINGS
            ),
            AppModel(
                packageName = "system.sound",
                activityName = "Settings.Sound",
                label = "Sound",
                icon = dummyDrawable,
                accentColor = 0xFF06D6A0.toInt(), // Emerald
                isSystemShortcut = true,
                systemIntentAction = Settings.ACTION_SOUND_SETTINGS
            ),
            AppModel(
                packageName = "system.bluetooth",
                activityName = "Settings.Bluetooth",
                label = "Bluetooth",
                icon = dummyDrawable,
                accentColor = 0xFF4361EE.toInt(), // Electric blue
                isSystemShortcut = true,
                systemIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
            ),
            AppModel(
                packageName = "system.settings",
                activityName = "Settings.Main",
                label = "All Settings",
                icon = dummyDrawable,
                accentColor = 0xFFFF9F1C.toInt(), // Warm amber
                isSystemShortcut = true,
                systemIntentAction = Settings.ACTION_SETTINGS
            ),
            AppModel(
                packageName = "system.cleantv",
                activityName = "CleanTV.Optimizer",
                label = "CleanTV Optimizer",
                icon = dummyDrawable,
                accentColor = 0xFF10B981.toInt(), // Vibrant Emerald
                isSystemShortcut = true,
                systemIntentAction = "com.example.firelauncher.action.CLEANTV"
            )
        )
    }

    /**
     * Launches an app using an explicit ComponentName and FLAG_ACTIVITY_NEW_TASK,
     * or dispatches a system shortcut intent.
     */
    fun launchApp(app: AppModel): Boolean {
        return try {
            if (app.isSystemShortcut && app.systemIntentAction != null) {
                val intent = Intent(app.systemIntentAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(app.packageName, app.activityName)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                true
            }
        } catch (_: Exception) {
            // Fallback to leanback launcher if standard component launch fails
            try {
                val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(app.packageName, app.activityName)
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(leanbackIntent)
                true
            } catch (_: Exception) {
                // Secondary fallback: getLaunchIntentForPackage
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    true
                } else {
                    false
                }
            }
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/ui/LauncherViewModel.kt',
    name: 'LauncherViewModel.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'MVVM AndroidViewModel exposing apps, favorites, system shortcuts, and homeTick StateFlows',
    content: `package com.example.firelauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppRepository
import com.example.firelauncher.data.FavoritesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel for FireLauncher adhering to MVVM architecture.
 * Exposes StateFlows for apps, favorites, system shortcuts, and home-button tick events.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    val favorites: StateFlow<Set<String>> = favoritesStore.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val systemShortcuts: StateFlow<List<AppModel>> = MutableStateFlow(repository.getSystemShortcuts())

    // Combined favorites app list
    val favoriteApps: StateFlow<List<AppModel>> = combine(_apps, favorites) { allApps, favSet ->
        allApps.filter { favSet.contains(it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedApp = MutableStateFlow<AppModel?>(null)
    val focusedApp: StateFlow<AppModel?> = _focusedApp.asStateFlow()

    // Incremented whenever Home is pressed while launcher is active
    private val _homeTick = MutableStateFlow(0L)
    val homeTick: StateFlow<Long> = _homeTick.asStateFlow()

    // Weather StateFlow derived from DataStore selected city and unit
    val weather: StateFlow<com.example.firelauncher.data.WeatherData> = combine(
        favoritesStore.selectedCityFlow,
        favoritesStore.isCelsiusFlow
    ) { city, isCelsius ->
        com.example.firelauncher.data.WeatherData.forCity(city, isCelsius)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.example.firelauncher.data.WeatherData.forCity("Seattle", false)
    )

    val availableCities: List<String> = com.example.firelauncher.data.WeatherData.CITY_PRESETS.map { it.city }

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Apps dynamically filtered by the global search query
    val filteredApps: StateFlow<List<AppModel>> = combine(_apps, _searchQuery) { allApps, query ->
        if (query.isBlank()) {
            allApps
        } else {
            val trimmed = query.trim().lowercase()
            allApps.filter { app ->
                app.label.lowercase().contains(trimmed) || app.packageName.lowercase().contains(trimmed)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshApps()
    }

    /**
     * Refreshes the list of installed applications from PackageManager.
     * Invoked initially and on every onResume of the Activity.
     */
    fun refreshApps() {
        viewModelScope.launch {
            val list = repository.getInstalledApps()
            _apps.value = list
            if (_focusedApp.value == null && list.isNotEmpty()) {
                _focusedApp.value = list.first()
            }
        }
    }

    /**
     * Called when the Home button is pressed while the launcher is open.
     * Triggers the homeTick counter to notify UI to scroll to top and refocus the first card.
     */
    fun onHomePressed() {
        _homeTick.value += 1L
    }

    /**
     * Updates the currently focused app (for hero text and dynamic background color).
     */
    fun setFocusedApp(app: AppModel) {
        _focusedApp.value = app
    }

    /**
     * Toggles the favorite status of the given app and saves to DataStore.
     */
    fun toggleFavorite(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch {
            favoritesStore.toggleFavorite(app.packageName)
        }
    }

    /**
     * Updates the selected city for the top weather widget.
     */
    fun selectCity(city: String) {
        viewModelScope.launch {
            favoritesStore.setSelectedCity(city)
        }
    }

    /**
     * Toggles between Fahrenheit and Celsius.
     */
    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            favoritesStore.toggleTemperatureUnit()
        }
    }

    /**
     * Updates the active global search query string.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Activates or closes the global search bar.
     */
    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    /**
     * Clears the search query while keeping search active.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Launches the specified application or system settings shortcut.
     */
    fun launchApp(app: AppModel): Boolean {
        return repository.launchApp(app)
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/ui/components/TopBar.kt',
    name: 'TopBar.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'TopBar with brand, live clock/date, focusable Weather widget, and dedicated Global Search button',
    content: `package com.example.firelauncher.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top bar displaying:
 * - Launcher brand on the left
 * - Focusable Weather widget in the center
 * - Global Search icon button / expandable search input
 * - Dynamic clock (12h/24h) and formatted calendar date on the right.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    weather: WeatherData,
    availableCities: List<String> = WeatherData.CITY_PRESETS.map { it.city },
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onSelectCity: (String) -> Unit = {},
    onToggleUnit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf(getFormattedTime(context)) }
    var currentDateString by remember { mutableStateOf(getFormattedDate()) }
    var showCityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = getFormattedTime(context)
            currentDateString = getFormattedDate()
            delay(1000L)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand logo & title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(FireAmber, FireOrange))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "FireLauncher",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            // Center: Weather Widget or Search Input
            if (isSearchActive) {
                // Expanded TV Search Bar
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = { onToggleSearch(false) },
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 24.dp)
                )
            } else {
                // Top Weather Widget (Current weather for selected city)
                WeatherWidget(
                    weather = weather,
                    onClick = { showCityDialog = true },
                    onToggleUnit = onToggleUnit
                )
            }

            // Right: Search Trigger Button + Live Clock & Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dedicated Search Icon Trigger
                SearchTriggerButton(
                    isActive = isSearchActive,
                    onClick = { onToggleSearch(!isSearchActive) }
                )

                // Live clock and date
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = currentTimeString,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentDateString,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // City Selector Overlay Dialog
        if (showCityDialog) {
            CitySelectionDialog(
                currentCity = weather.city,
                cities = availableCities,
                onCitySelected = { city ->
                    onSelectCity(city)
                    showCityDialog = false
                },
                onDismiss = { showCityDialog = false }
            )
        }
    }
}

/**
 * Dedicated TV-Material focusable Search Icon button in the top bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchTriggerButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isActive) FireOrange else Color.White.copy(alpha = 0.15f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireOrange.copy(alpha = 0.8f),
                elevation = 16.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isActive) FireOrange.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (isActive) "Close Search" else "Search Apps",
                tint = if (isActive) FireOrange else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Expanded Search Bar with real-time text input and clear button.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GlobalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.7f)),
                shape = shape
            )
        ),
        modifier = modifier.width(380.dp).height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = FireOrange,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search installed apps...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FireOrange),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

/**
 * TV-Material focusable weather surface displaying weather icon, temp, condition, and selected city.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherWidget(
    weather: WeatherData,
    onClick: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireOrange.copy(alpha = 0.7f),
                elevation = 14.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val weatherIcon: ImageVector = when (weather.iconType) {
                "clear" -> Icons.Rounded.WbSunny
                "partly_cloudy" -> Icons.Rounded.WbCloudy
                "cloudy" -> Icons.Rounded.Cloud
                "rain" -> Icons.Rounded.WaterDrop
                "thunder" -> Icons.Rounded.Thunderstorm
                else -> Icons.Rounded.WbSunny
            }
            val iconTint = when (weather.iconType) {
                "clear" -> FireAmber
                "partly_cloudy" -> Color(0xFF90E0EF)
                "rain" -> Color(0xFF00B4D8)
                else -> Color(0xFFE0E0E0)
            }

            androidx.compose.material3.Icon(
                imageVector = weatherIcon,
                contentDescription = weather.condition,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather.displayTemperature,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.condition,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Location",
                        tint = FireOrange,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = weather.city,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FireAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.displayHighLow,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * TV dialog modal allowing user to change the selected city with D-pad navigation.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    currentCity: String,
    cities: List<String>,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            onClick = {},
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF141824)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                )
            ),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Weather Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Close",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TvLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(cities) { city ->
                        val isSelected = city.equals(currentCity, ignoreCase = true)
                        Surface(
                            onClick = { onCitySelected(city) },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) FireOrange.copy(alpha = 0.2f) else Color.Transparent,
                                focusedContainerColor = DarkSurfaceVariant
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FireAmber else TextPrimary
                                )
                                if (isSelected) {
                                    Text(
                                        text = "SELECTED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FireAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getFormattedTime(context: Context): String {
    val is24Hour = DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date())
}

private fun getFormattedDate(): String {
    val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return formatter.format(Date())
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/ui/components/AppCard.kt',
    name: 'AppCard.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: '16:9 Surface card: 1.12x focus scale, white border, tinted glow, yellow dot, and hold-to-favorite',
    content: `package com.example.firelauncher.ui.components

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FavoriteYellow
import com.example.firelauncher.ui.theme.TextPrimary

/**
 * 16:9 App card built using tv-material Surface.
 * On D-pad focus:
 * - Scales to 1.12
 * - White 2dp border
 * - Accent-color tinted elevation glow
 * - Shows small yellow dot if favorited
 * - Handles OK click to launch, and Long-press OK to toggle favorite
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: AppModel,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val accentColor = Color(app.accentColor)
    val cardShape = RoundedCornerShape(12.dp)
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = cardShape,
            focusedShape = cardShape
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.12f
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = cardShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = cardShape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = accentColor.copy(alpha = 0.85f),
                elevation = 20.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                // If it's a system shortcut, render clean icon + title
                app.isSystemShortcut -> {
                    SystemShortcutContent(app = app, accentColor = accentColor)
                }

                // If app has Leanback banner (320x180), show the banner
                app.banner != null -> {
                    BannerContent(banner = app.banner, label = app.label)
                }

                // Standard app without banner: Icon centered + title at bottom
                else -> {
                    IconFallbackContent(icon = app.icon, label = app.label, accentColor = accentColor)
                }
            }

            // Yellow favorite indicator dot in top-right corner
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(FavoriteYellow)
                )
            }
        }
    }
}

@Composable
private fun SystemShortcutContent(
    app: AppModel,
    accentColor: Color
) {
    val iconVector: ImageVector = when (app.label.lowercase()) {
        "wi-fi" -> Icons.Rounded.Wifi
        "display" -> Icons.Rounded.DisplaySettings
        "sound" -> Icons.Rounded.VolumeUp
        "bluetooth" -> Icons.Rounded.Bluetooth
        else -> Icons.Rounded.Settings
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.25f),
                        DarkSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = iconVector,
                contentDescription = app.label,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BannerContent(
    banner: Drawable,
    label: String
) {
    val bitmap = remember(banner) {
        try {
            banner.toBitmap(width = 320, height = 180)
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun IconFallbackContent(
    icon: Drawable,
    label: String,
    accentColor: Color
) {
    val bitmap = remember(icon) {
        try {
            icon.toBitmap(width = 96, height = 96)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.15f),
                        DarkSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = label,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/ui/LauncherRoot.kt',
    name: 'LauncherRoot.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Dynamic background gradient, 48dp overscan padding, search filtering, BackHandler, and Home scroll-to-top handler',
    content: `package com.example.firelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.components.AppRow
import com.example.firelauncher.ui.components.HeroSection
import com.example.firelauncher.ui.components.TopBar
import com.example.firelauncher.ui.theme.DarkBackground
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, handling TV overscan padding,
 * and intercepting Back/Home events.
 */
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    // Intercept Back button: if search is active, close search; otherwise ignore
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    BackHandler {
        if (isSearchActive) {
            viewModel.setSearchActive(false)
        }
    }

    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val systemShortcuts by viewModel.systemShortcuts.collectAsStateWithLifecycle()
    val focusedApp by viewModel.focusedApp.collectAsStateWithLifecycle()
    val homeTick by viewModel.homeTick.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()

    LauncherContent(
        apps = apps,
        favorites = favorites,
        favoriteApps = favoriteApps,
        systemShortcuts = systemShortcuts,
        focusedApp = focusedApp,
        homeTick = homeTick,
        weather = weather,
        availableCities = viewModel.availableCities,
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        filteredApps = filteredApps,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onToggleSearch = { viewModel.setSearchActive(it) },
        onSelectCity = { viewModel.selectCity(it) },
        onToggleUnit = { viewModel.toggleTemperatureUnit() },
        onAppFocused = { viewModel.setFocusedApp(it) },
        onAppClicked = { viewModel.launchApp(it) },
        onAppLongClicked = { viewModel.toggleFavorite(it) },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the complete Fire TV launcher interface.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherContent(
    apps: List<AppModel>,
    favorites: Set<String>,
    favoriteApps: List<AppModel>,
    systemShortcuts: List<AppModel>,
    focusedApp: AppModel?,
    homeTick: Long,
    weather: com.example.firelauncher.data.WeatherData,
    availableCities: List<String>,
    searchQuery: String,
    isSearchActive: Boolean,
    filteredApps: List<AppModel>,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSelectCity: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnState = rememberTvLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }

    // Smooth background gradient transition to the focused app's clamped accent color
    val targetAccentColor = focusedApp?.let { Color(it.accentColor) } ?: FireOrange
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 450),
        label = "BackgroundAccentAnimation"
    )

    // Request initial focus on the first card once apps are loaded
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus will settle on first user input if requester not yet attached
            }
        }
    }

    // When Home is pressed while launcher is open: scroll to top and refocus first card
    LaunchedEffect(homeTick) {
        if (homeTick > 0L) {
            columnState.scrollToItem(0)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Safe catch if layout is re-composing
            }
        }
    }

    // Overscan-safe layout with 48dp horizontal margins
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.32f),
                        animatedAccentColor.copy(alpha = 0.12f),
                        DarkBackground.copy(alpha = 0.95f),
                        DarkBackground
                    ),
                    startY = 0f,
                    endY = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp) // 48dp TV overscan safe padding
                .padding(top = 28.dp, bottom = 20.dp)
        ) {
            // Top bar: App name, Weather widget, Global Search & live clock/date
            TopBar(
                weather = weather,
                availableCities = availableCities,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onSelectCity = onSelectCity,
                onToggleUnit = onToggleUnit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hero section: Focused app title and dynamic hints
            HeroSection(
                focusedApp = focusedApp,
                isFavorite = focusedApp != null && favorites.contains(focusedApp.packageName)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable rows of apps or filtered search results
            TvLazyColumn(
                state = columnState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearchActive || searchQuery.isNotBlank()) {
                    // Global Search Mode
                    if (filteredApps.isNotEmpty()) {
                        item(key = "search_results") {
                            AppRow(
                                title = if (searchQuery.isBlank()) "All Apps (\${filteredApps.size})" else "Search Results (\${filteredApps.size})",
                                apps = filteredApps,
                                favorites = favorites,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onAppFocused = onAppFocused,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked
                            )
                        }
                    } else {
                        // Empty search results state
                        item(key = "empty_search") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = "No results",
                                    tint = TextSecondary,
                                    modifier = Modifier.height(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No apps found matching \"$searchQuery\"",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Check the spelling or try searching for another app name",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Standard Home Launcher Rows
                    // Row 1: Favorites (Only visible when non-empty)
                    if (favoriteApps.isNotEmpty()) {
                        item(key = "row_favorites") {
                            AppRow(
                                title = "Favorites",
                                apps = favoriteApps,
                                favorites = favorites,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onAppFocused = onAppFocused,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked
                            )
                        }
                    }

                    // Row 2: All Apps (alphabetical)
                    item(key = "row_all_apps") {
                        AppRow(
                            title = "All Apps",
                            apps = apps,
                            favorites = favorites,
                            firstItemFocusRequester = if (favoriteApps.isEmpty()) firstItemFocusRequester else null,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
                    }

                    // Row 3: System Shortcuts
                    item(key = "row_system") {
                        AppRow(
                            title = "System",
                            apps = systemShortcuts,
                            favorites = favorites,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
                    }
                }

                // Bottom safety spacer for overscan visibility
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/MainActivity.kt',
    name: 'MainActivity.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'SingleTask ComponentActivity with onNewIntent Home handler, onResume refresh, and Back key absorption',
    content: `package com.example.firelauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.firelauncher.ui.LauncherRoot
import com.example.firelauncher.ui.LauncherViewModel
import com.example.firelauncher.ui.theme.FireLauncherTheme

/**
 * Single-task main Activity acting as Leanback and Home launcher for Fire OS.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock window to immersive TV fullscreen
        hideSystemUI()

        setContent {
            FireLauncherTheme {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        // Refresh installed apps whenever launcher returns to foreground
        viewModel.refreshApps()
    }

    /**
     * When the user presses the Home key while FireLauncher is already running,
     * the system delivers an Intent to onNewIntent (due to launchMode="singleTask").
     * We catch this and scroll to the top of the launcher and refocus the first item.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            viewModel.onHomePressed()
        }
    }

    /**
     * Fire OS home screen launchers must ignore the back button,
     * as there is no previous activity in the backstack.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Deliberately ignored on home launcher
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Absorb back press
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun hideSystemUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }
}
`
  },
  {
    path: 'README.md',
    name: 'README.md',
    language: 'markdown',
    category: 'docs',
    description: 'Documentation: ADB wireless connect, app-debug.apk installation, and Fire OS default launcher note',
    content: `# FireLauncher 🔥

A high-performance custom home-screen launcher designed specifically for **Amazon Fire TV (Fire OS)**, built in Kotlin using **Jetpack Compose for TV (tv-material)**, **MVVM architecture**, **Preferences DataStore**, and **AndroidX Palette** for dynamic ambient theming.

See complete repository instructions and ADB installation steps.
`
  }
];
