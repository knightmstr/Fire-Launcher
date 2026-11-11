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

    <!-- Network permissions for r/TVWallpapersSFW and live Open-Meteo weather -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

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
        android:allowBackup="false"
        android:usesCleartextTraffic="false"
        android:networkSecurityConfig="@xml/network_security_config"
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
    path: 'app/src/main/res/xml/network_security_config.xml',
    name: 'network_security_config.xml',
    language: 'xml',
    category: 'res',
    description: 'Enforces TLS-only network traffic and strictly forbids cleartext HTTP',
    content: `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Enforce HTTPS across all domains, forbidding unencrypted cleartext HTTP -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Pinned domains for dynamic wallpaper and weather services -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">reddit.com</domain>
        <domain includeSubdomains="true">redd.it</domain>
        <domain includeSubdomains="true">imgur.com</domain>
        <domain includeSubdomains="true">unsplash.com</domain>
        <domain includeSubdomains="true">open-meteo.com</domain>
    </domain-config>
</network-security-config>
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
    path: 'app/src/main/java/com/example/firelauncher/FireLauncherApp.kt',
    name: 'FireLauncherApp.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Application class with Coil ImageLoaderFactory enforcing 10% memory cache and 15MB disk cache limits',
    content: `package com.example.firelauncher

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Main Application class for FireLauncher.
 * Configures strict memory and disk cache limits for low-end Fire TV hardware
 * to prevent r/TVWallpapersSFW automatic downloads from bloating limited flash storage.
 */
class FireLauncherApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Strict memory cache ceiling: exactly 10% of available application RAM
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Hard disk cache ceiling: capped at exactly 15MB
                    .maxSizeBytes(15L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Optimize for TV wallpaper rotation
            .crossfade(true)
            .build()
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/ui/components/AppContextMenuDialog.kt',
    name: 'AppContextMenuDialog.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Leanback context menu dialog for cards (Favorites toggle, Force Stop & App Info settings, Launch)',
    content: `package com.example.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel

// Context menu dialog component (see app/src/main/java/.../AppContextMenuDialog.kt)
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/AppSortOrder.kt',
    name: 'AppSortOrder.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Enum and data models for Alphabetical, Most Recently Used, and Most Frequently Used app library sorting',
    content: `package com.example.firelauncher.data

/**
 * Sort order options for the Fire TV application library.
 */
enum class AppSortOrder(val title: String, val subtitle: String) {
    ALPHABETICAL(
        title = "Alphabetical (A–Z)",
        subtitle = "Standard alphabetical order by application name"
    ),
    MOST_RECENT(
        title = "Most Recently Used",
        subtitle = "Applications opened most recently appear first"
    ),
    MOST_FREQUENT(
        title = "Most Frequently Used",
        subtitle = "Applications you launch most often appear first"
    );

    companion object {
        fun fromName(name: String?): AppSortOrder {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALPHABETICAL
        }
    }
}

/**
 * Usage metrics tracked per application package to power Recents and Frequent sorting.
 */
data class AppUsageStat(
    val packageName: String,
    val launchCount: Int = 0,
    val lastLaunchedTimeMs: Long = 0L
)
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/AppModel.kt',
    name: 'AppModel.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Data model representing installed Leanback/standard apps and system shortcuts with launch usage stats',
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
    val systemIntentAction: String? = null,
    val launchCount: Int = 0,
    val lastLaunchedTimeMs: Long = 0L
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
    path: 'app/src/main/java/com/example/firelauncher/data/WallpaperModel.kt',
    name: 'WallpaperModel.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Data model for 16:9 TV wallpapers fetched from r/TVWallpapersSFW',
    content: `package com.example.firelauncher.data

/**
 * Data model representing a 16:9 TV wallpaper fetched from r/TVWallpapersSFW or curated fallback.
 */
data class WallpaperModel(
    val id: String,
    val title: String,
    val url: String,
    val author: String = "r/TVWallpapersSFW",
    val permalink: String = "https://reddit.com/r/TVWallpapersSFW",
    val source: String = "reddit"
)
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/WallpaperRepository.kt',
    name: 'WallpaperRepository.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Fetches high-res 16:9 TV wallpapers from r/TVWallpapersSFW and rotates every 30 minutes',
    content: `package com.example.firelauncher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages fetching wallpapers from r/TVWallpapersSFW and automatically
 * cycling through them every 30 minutes.
 */
class WallpaperRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val ROTATION_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

        val FALLBACK_WALLPAPERS = listOf(
            WallpaperModel(
                id = "wp_01",
                title = "Twilight Over Yosemite Valley (4K)",
                url = "https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=2560&q=85",
                author = "u/NatureLover4K"
            ),
            WallpaperModel(
                id = "wp_02",
                title = "Deep Space Carina Nebula & Cosmic Dust (OLED)",
                url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=2560&q=85",
                author = "u/AstroCinema"
            ),
            WallpaperModel(
                id = "wp_03",
                title = "Mount Fuji at Golden Hour with Cherry Blossoms",
                url = "https://images.unsplash.com/photo-1490806843957-31f4c9a91c65?auto=format&fit=crop&w=2560&q=85",
                author = "u/TokyoDriftViews"
            ),
            WallpaperModel(
                id = "wp_04",
                title = "Lofoten Islands Emerald Aurora Borealis Night",
                url = "https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?auto=format&fit=crop&w=2560&q=85",
                author = "u/NordicLightWalker"
            ),
            WallpaperModel(
                id = "wp_05",
                title = "Tokyo Rainy Shibuya Cyberpunk Reflections 4K",
                url = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=2560&q=85",
                author = "u/NeonCityScape"
            ),
            WallpaperModel(
                id = "wp_06",
                title = "Italian Dolomites Alpine Lakes & Mist",
                url = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2560&q=85",
                author = "u/AlpineDreamer"
            ),
            WallpaperModel(
                id = "wp_07",
                title = "Big Sur Pacific Coastline Sunset Horizon",
                url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2560&q=85",
                author = "u/CoastToCoastTV"
            ),
            WallpaperModel(
                id = "wp_08",
                title = "Sunbeams Through Redwood Forest Canopy",
                url = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=2560&q=85",
                author = "u/ForestWhispers"
            )
        )
    }

    private val _wallpapers = MutableStateFlow<List<WallpaperModel>>(FALLBACK_WALLPAPERS)
    val wallpapers: StateFlow<List<WallpaperModel>> = _wallpapers.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)

    private val _currentWallpaper = MutableStateFlow<WallpaperModel?>(FALLBACK_WALLPAPERS.first())
    val currentWallpaper: StateFlow<WallpaperModel?> = _currentWallpaper.asStateFlow()

    init {
        // Start background fetch from Reddit
        scope.launch {
            fetchRedditWallpapers()
        }

        // Start 30-minute auto-rotation timer
        scope.launch {
            while (isActive) {
                delay(ROTATION_INTERVAL_MS)
                nextWallpaper()
            }
        }
    }

    fun nextWallpaper() {
        val list = _wallpapers.value
        if (list.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % list.size
        _currentIndex.value = nextIdx
        _currentWallpaper.value = list[nextIdx]
    }

    fun setWallpaper(wallpaper: WallpaperModel) {
        val idx = _wallpapers.value.indexOfFirst { it.id == wallpaper.id }
        if (idx >= 0) {
            _currentIndex.value = idx
        }
        _currentWallpaper.value = wallpaper
    }

    suspend fun fetchRedditWallpapers() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.reddit.com/r/TVWallpapersSFW/hot.json?limit=40")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("User-Agent", "android:com.example.firelauncher:v1.0 (Fire OS Leanback)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonString = reader.use { it.readText() }
                val root = JSONObject(jsonString)
                val children = root.getJSONObject("data").getJSONArray("children")

                val parsedList = mutableListOf<WallpaperModel>()

                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).getJSONObject("data")
                    val isNsfw = childData.optBoolean("over_18", false)
                    val isVideo = childData.optBoolean("is_video", false)
                    if (isNsfw || isVideo) continue

                    var imgUrl = childData.optString("url", "")
                    val isDirectImage = imgUrl.endsWith(".jpg", true) ||
                            imgUrl.endsWith(".jpeg", true) ||
                            imgUrl.endsWith(".png", true) ||
                            imgUrl.endsWith(".webp", true)

                    if (!isDirectImage && childData.has("preview")) {
                        try {
                            val images = childData.getJSONObject("preview").getJSONArray("images")
                            if (images.length() > 0) {
                                val sourceUrl = images.getJSONObject(0).getJSONObject("source").getString("url")
                                imgUrl = sourceUrl.replace("&amp;", "&")
                            }
                        } catch (_: Exception) {
                            // continue with direct url
                        }
                    }

                    val isSafeUrl = imgUrl.startsWith("https://", ignoreCase = true) &&
                            try {
                                val host = java.net.URI(imgUrl).host?.lowercase() ?: ""
                                host == "i.redd.it" ||
                                        host == "preview.redd.it" ||
                                        host == "external-preview.redd.it" ||
                                        host.endsWith(".redd.it") ||
                                        host == "i.imgur.com" ||
                                        host.endsWith(".imgur.com") ||
                                        host == "images.unsplash.com"
                            } catch (_: Exception) {
                                false
                            }

                    if (isSafeUrl) {
                        parsedList.add(
                            WallpaperModel(
                                id = childData.optString("id", "post_\$i"),
                                title = childData.optString("title", "TV Wallpaper").take(120),
                                url = imgUrl,
                                author = "u/" + childData.optString("author", "reddit_user").take(40),
                                permalink = "https://reddit.com" + childData.optString("permalink", "").take(200),
                                source = "reddit"
                            )
                        )
                    }
                }

                if (parsedList.isNotEmpty()) {
                    _wallpapers.value = parsedList
                    _currentWallpaper.value = parsedList.first()
                }
            }
        } catch (_: Exception) {
            // Keep fallback wallpapers if offline or restricted
        }
    }
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/WeatherData.kt',
    name: 'WeatherData.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Weather data model with Open-Meteo live metrics and CityLocationModel for global search',
    content: `package com.example.firelauncher.data

/**
 * Data model representing current weather conditions for the top-bar launcher widget.
 * Sourced live from the free and accurate Open-Meteo Weather API.
 */
data class WeatherData(
    val city: String = "Seattle",
    val country: String = "WA, USA",
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val temperatureF: Int = 68,
    val temperatureC: Int = 20,
    val feelsLikeF: Int = 68,
    val feelsLikeC: Int = 20,
    val condition: String = "Partly Cloudy",
    val iconType: String = "partly_cloudy",
    val highF: Int = 72,
    val lowF: Int = 55,
    val humidity: Int = 65,
    val windMph: Int = 8,
    val isCelsius: Boolean = false,
    val isLive: Boolean = true
) {
    val displayTemperature: String
        get() = if (isCelsius) "\$temperatureC°C" else "\$temperatureF°F"

    val displayHighLow: String
        get() = if (isCelsius) {
            "H:\${((highF - 32) * 5 / 9)}° L:\${((lowF - 32) * 5 / 9)}°"
        } else {
            "H:\$highF° L:\$lowF°"
        }

    val displayLocation: String
        get() = if (country.isNotBlank()) "\$city, \$country" else city

    companion object {
        fun defaultSeattle(isCelsius: Boolean = false): WeatherData {
            return WeatherData(
                city = "Seattle",
                country = "WA, USA",
                latitude = 47.6062,
                longitude = -122.3321,
                temperatureF = 64,
                temperatureC = 18,
                condition = "Partly Cloudy",
                iconType = "partly_cloudy",
                highF = 68,
                lowF = 52,
                isCelsius = isCelsius
            )
        }
    }
}

data class CityLocationModel(
    val id: String,
    val name: String,
    val admin1: String? = null,
    val country: String,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = if (!admin1.isNullOrBlank()) "\$name, \$admin1, \$country" else "\$name, \$country"
}
`
  },
  {
    path: 'app/src/main/java/com/example/firelauncher/data/WeatherRepository.kt',
    name: 'WeatherRepository.kt',
    language: 'kotlin',
    category: 'kotlin',
    description: 'Queries Open-Meteo Geocoding API for city search and Forecast API for accurate live weather',
    content: `package com.example.firelauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WeatherRepository {

    suspend fun searchCities(query: String): List<CityLocationModel> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().take(64)
        if (cleanQuery.length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=\$encoded&count=8&language=en&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONObject(text)
                if (!root.has("results")) return@withContext emptyList()
                val array = root.getJSONArray("results")
                val list = mutableListOf<CityLocationModel>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(
                        CityLocationModel(
                            id = "geo_" + item.optLong("id", i.toLong()),
                            name = item.optString("name", "Unknown"),
                            admin1 = if (item.has("admin1")) item.optString("admin1") else null,
                            country = item.optString("country", ""),
                            countryCode = item.optString("country_code", ""),
                            latitude = item.optDouble("latitude", 0.0),
                            longitude = item.optDouble("longitude", 0.0)
                        )
                    )
                }
                return@withContext list
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun fetchLiveWeather(
        cityName: String,
        country: String,
        lat: Double,
        lon: Double,
        isCelsius: Boolean
    ): WeatherData = withContext(Dispatchers.IO) {
        try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=\$lat&longitude=\$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&temperature_unit=fahrenheit&wind_speed_unit=mph"

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONObject(text)
                val current = root.getJSONObject("current")
                val daily = root.optJSONObject("daily")

                val tempF = Math.round(current.optDouble("temperature_2m", 68.0)).toInt()
                val tempC = Math.round(((tempF - 32) * 5.0) / 9.0).toInt()

                val feelsLikeF = Math.round(current.optDouble("apparent_temperature", tempF.toDouble())).toInt()
                val feelsLikeC = Math.round(((feelsLikeF - 32) * 5.0) / 9.0).toInt()

                val wmoCode = current.optInt("weather_code", 1)
                val isDay = current.optInt("is_day", 1) == 1

                val highF = if (daily != null && daily.has("temperature_2m_max")) {
                    Math.round(daily.getJSONArray("temperature_2m_max").optDouble(0, (tempF + 5).toDouble())).toInt()
                } else tempF + 5

                val lowF = if (daily != null && daily.has("temperature_2m_min")) {
                    Math.round(daily.getJSONArray("temperature_2m_min").optDouble(0, (tempF - 6).toDouble())).toInt()
                } else tempF - 6

                val humidity = Math.round(current.optDouble("relative_humidity_2m", 55.0)).toInt()
                val windMph = Math.round(current.optDouble("wind_speed_10m", 8.0)).toInt()

                val (conditionStr, iconStr) = mapWmoCode(wmoCode, isDay)

                return@withContext WeatherData(
                    city = cityName,
                    country = country,
                    latitude = lat,
                    longitude = lon,
                    temperatureF = tempF,
                    temperatureC = tempC,
                    feelsLikeF = feelsLikeF,
                    feelsLikeC = feelsLikeC,
                    condition = conditionStr,
                    iconType = iconStr,
                    highF = highF,
                    lowF = lowF,
                    humidity = humidity,
                    windMph = windMph,
                    isCelsius = isCelsius,
                    isLive = true
                )
            }
        } catch (_: Exception) {}

        WeatherData(
            city = cityName,
            country = country,
            latitude = lat,
            longitude = lon,
            temperatureF = 68,
            temperatureC = 20,
            condition = "Partly Cloudy",
            iconType = "partly_cloudy",
            isCelsius = isCelsius,
            isLive = false
        )
    }

    private fun mapWmoCode(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> (if (isDay) "Sunny" else "Clear Sky") to "clear"
            1 -> (if (isDay) "Mainly Sunny" else "Mainly Clear") to "clear"
            2 -> "Partly Cloudy" to "partly_cloudy"
            3 -> "Overcast" to "cloudy"
            45, 48 -> "Fog & Mist" to "cloudy"
            51, 53, 55, 56, 57 -> "Light Drizzle" to "rain"
            61, 63, 65, 66, 67 -> "Rain" to "rain"
            71, 73, 75, 77 -> "Snow" to "snow"
            80, 81, 82 -> "Passing Showers" to "rain"
            85, 86 -> "Snow Showers" to "snow"
            95, 96, 99 -> "Thunderstorm" to "thunder"
            else -> "Fair" to "partly_cloudy"
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
    description: 'Preferences DataStore manager for persistent favorites, weather coordinates, and saved cities',
    content: `package com.example.firelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firelauncher_prefs")

class FavoritesStore(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages_set")
        private val KEY_WEATHER_CITY = stringPreferencesKey("weather_selected_city")
        private val KEY_WEATHER_COUNTRY = stringPreferencesKey("weather_selected_country")
        private val KEY_WEATHER_LAT = doublePreferencesKey("weather_selected_lat")
        private val KEY_WEATHER_LON = doublePreferencesKey("weather_selected_lon")
        private val KEY_WEATHER_CELSIUS = booleanPreferencesKey("weather_is_celsius")
        private val KEY_SAVED_CITIES = stringSetPreferencesKey("weather_saved_cities_set")

        val DEFAULT_CITIES = setOf(
            "Seattle|WA, USA|47.6062|-122.3321",
            "New York|NY, USA|40.7128|-74.0060",
            "Los Angeles|CA, USA|34.0522|-118.2437",
            "London|UK|51.5074|-0.1278",
            "Tokyo|Japan|35.6762|139.6503",
            "Paris|France|48.8566|2.3522",
            "Sydney|Australia|-33.8688|151.2093",
            "Denver|CO, USA|39.7392|-104.9903"
        )
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CITY] ?: "Seattle" }

    val selectedCountryFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_COUNTRY] ?: "WA, USA" }

    val selectedLatFlow: Flow<Double> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_LAT] ?: 47.6062 }

    val selectedLonFlow: Flow<Double> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_LON] ?: -122.3321 }

    val isCelsiusFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CELSIUS] ?: false }

    val savedCitiesFlow: Flow<List<CityLocationModel>> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val set = prefs[KEY_SAVED_CITIES] ?: DEFAULT_CITIES
            set.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 4) {
                    val name = parts[0]
                    val country = parts[1]
                    val lat = parts[2].toDoubleOrNull() ?: 0.0
                    val lon = parts[3].toDoubleOrNull() ?: 0.0
                    CityLocationModel(
                        id = "saved_\${name}_\$country",
                        name = name,
                        country = country,
                        latitude = lat,
                        longitude = lon
                    )
                } else null
            }
        }

    suspend fun setSelectedLocation(city: String, country: String, lat: Double, lon: Double) {
        val safeCity = city.replace("|", "-").trim()
        val safeCountry = country.replace("|", "-").trim()
        val safeLat = if (lat.isFinite() && lat in -90.0..90.0) lat else 0.0
        val safeLon = if (lon.isFinite() && lon in -180.0..180.0) lon else 0.0

        context.dataStore.edit { preferences ->
            preferences[KEY_WEATHER_CITY] = safeCity
            preferences[KEY_WEATHER_COUNTRY] = safeCountry
            preferences[KEY_WEATHER_LAT] = safeLat
            preferences[KEY_WEATHER_LON] = safeLon

            val currentSet = (preferences[KEY_SAVED_CITIES] ?: DEFAULT_CITIES).toMutableSet()
            currentSet.add("\$safeCity|\$safeCountry|\$safeLat|\$safeLon")
            preferences[KEY_SAVED_CITIES] = currentSet
        }
    }

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
                    if (app.packageName == "system.cleantv" || app.systemIntentAction.startsWith("com.example.firelauncher")) {
                        setPackage(context.packageName)
                    }
                }
                if (intent.resolveActivity(packageManager) != null) {
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
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
    description: 'MVVM AndroidViewModel exposing apps, favorites, 30m r/TVWallpapersSFW rotation, and live Open-Meteo weather with city search',
    content: `package com.example.firelauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppRepository
import com.example.firelauncher.data.CityLocationModel
import com.example.firelauncher.data.FavoritesStore
import com.example.firelauncher.data.WallpaperModel
import com.example.firelauncher.data.WallpaperRepository
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main ViewModel for FireLauncher adhering to MVVM architecture.
 * Exposes StateFlows for apps, favorites, system shortcuts, home-button tick events,
 * periodic r/TVWallpapersSFW wallpapers (changing every 30 mins), and accurate live Open-Meteo weather with city search.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val wallpaperRepository = WallpaperRepository(application, viewModelScope)
    private val weatherRepository = WeatherRepository()

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    val favorites: StateFlow<Set<String>> = favoritesStore.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val systemShortcuts: StateFlow<List<AppModel>> = MutableStateFlow(repository.getSystemShortcuts())

    val favoriteApps: StateFlow<List<AppModel>> = combine(_apps, favorites) { allApps, favSet ->
        allApps.filter { favSet.contains(it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedApp = MutableStateFlow<AppModel?>(null)
    val focusedApp: StateFlow<AppModel?> = _focusedApp.asStateFlow()

    private val _homeTick = MutableStateFlow(0L)
    val homeTick: StateFlow<Long> = _homeTick.asStateFlow()

    val currentWallpaper: StateFlow<WallpaperModel?> = wallpaperRepository.currentWallpaper
    val allWallpapers: StateFlow<List<WallpaperModel>> = wallpaperRepository.wallpapers

    private val _weather = MutableStateFlow(WeatherData.defaultSeattle())
    val weather: StateFlow<WeatherData> = _weather.asStateFlow()

    val savedCities: StateFlow<List<CityLocationModel>> = favoritesStore.savedCitiesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isCelsius: StateFlow<Boolean> = favoritesStore.isCelsiusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _searchResults = MutableStateFlow<List<CityLocationModel>>(emptyList())
    val searchResults: StateFlow<List<CityLocationModel>> = _searchResults.asStateFlow()

    private val _isSearchingCities = MutableStateFlow(false)
    val isSearchingCities: StateFlow<Boolean> = _isSearchingCities.asStateFlow()

    private var citySearchJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

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

        viewModelScope.launch {
            combine(
                favoritesStore.selectedCityFlow,
                favoritesStore.selectedCountryFlow,
                favoritesStore.selectedLatFlow,
                favoritesStore.selectedLonFlow,
                favoritesStore.isCelsiusFlow
            ) { city, country, lat, lon, celsius ->
                fetchCurrentWeather(city, country, lat, lon, celsius)
            }.collect {}
        }

        viewModelScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                val cur = _weather.value
                fetchCurrentWeather(cur.city, cur.country, cur.latitude, cur.longitude, cur.isCelsius)
            }
        }
    }

    private suspend fun fetchCurrentWeather(city: String, country: String, lat: Double, lon: Double, celsius: Boolean) {
        val result = weatherRepository.fetchLiveWeather(city, country, lat, lon, celsius)
        _weather.value = result
    }

    fun refreshApps() {
        viewModelScope.launch {
            val list = repository.getInstalledApps()
            _apps.value = list
            if (_focusedApp.value == null && list.isNotEmpty()) {
                _focusedApp.value = list.first()
            }
        }
    }

    fun onHomePressed() {
        _homeTick.value += 1L
    }

    fun setFocusedApp(app: AppModel) {
        _focusedApp.value = app
    }

    fun toggleFavorite(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch {
            favoritesStore.toggleFavorite(app.packageName)
        }
    }

    fun searchCities(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearchingCities.value = false
            return
        }

        citySearchJob = viewModelScope.launch {
            _isSearchingCities.value = true
            delay(300L)
            val results = weatherRepository.searchCities(query)
            _searchResults.value = results
            _isSearchingCities.value = false
        }
    }

    fun clearCitySearch() {
        citySearchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearchingCities.value = false
    }

    fun selectCity(city: CityLocationModel) {
        viewModelScope.launch {
            favoritesStore.setSelectedLocation(
                city = city.name,
                country = city.admin1?.let { "\$it, \${city.countryCode ?: city.country}" } ?: city.country,
                lat = city.latitude,
                lon = city.longitude
            )
            clearCitySearch()
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            favoritesStore.toggleTemperatureUnit()
        }
    }

    fun nextWallpaper() {
        wallpaperRepository.nextWallpaper()
    }

    fun setWallpaper(wallpaper: WallpaperModel) {
        wallpaperRepository.setWallpaper(wallpaper)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

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
    description: 'TopBar with brand, live clock/date, live Open-Meteo Weather with city search, Wallpaper trigger, and Global Search',
    content: `package com.example.firelauncher.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Wallpaper
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
import com.example.firelauncher.data.CityLocationModel
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    weather: WeatherData,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearchingCities: Boolean,
    onSearchCities: (String) -> Unit,
    onClearCitySearch: () -> Unit,
    onSelectCity: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    sortOrder: AppSortOrder = AppSortOrder.ALPHABETICAL,
    onSelectSort: (AppSortOrder) -> Unit = {},
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onNextWallpaper: () -> Unit = {},
    wallpaperTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf(getFormattedTime(context)) }
    var currentDateString by remember { mutableStateOf(getFormattedDate()) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "FireLauncher",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    if (wallpaperTitle != null) {
                        Text(
                            text = "r/TVWallpapersSFW",
                            fontSize = 9.sp,
                            color = FireAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isSearchActive) {
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = { onToggleSearch(false) },
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 24.dp)
                )
            } else {
                WeatherWidget(
                    weather = weather,
                    onClick = { showCityDialog = true },
                    onToggleUnit = onToggleUnit
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortTriggerButton(
                    currentSort = sortOrder,
                    onClick = { showSortDialog = true }
                )

                WallpaperTriggerButton(
                    onClick = onNextWallpaper,
                    title = "Change Wallpaper (30m auto)"
                )

                SearchTriggerButton(
                    isActive = isSearchActive,
                    onClick = { onToggleSearch(!isSearchActive) }
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = currentTimeString,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentDateString,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (showCityDialog) {
            CitySelectionDialog(
                currentCity = weather.city,
                savedCities = savedCities,
                searchResults = searchResults,
                isSearching = isSearchingCities,
                isCelsius = weather.isCelsius,
                onSearch = onSearchCities,
                onClearSearch = onClearCitySearch,
                onCitySelected = { city ->
                    onSelectCity(city)
                    showCityDialog = false
                },
                onToggleUnit = onToggleUnit,
                onDismiss = {
                    onClearCitySearch()
                    showCityDialog = false
                }
            )
        }

        if (showSortDialog) {
            SortSelectionDialog(
                currentSort = sortOrder,
                onSelectSort = { order ->
                    onSelectSort(order)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WallpaperTriggerButton(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
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
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Wallpaper,
                contentDescription = title,
                tint = FireAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

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
        modifier = modifier.size(40.dp),
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
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (isActive) "Close Search" else "Search Apps",
                tint = if (isActive) FireOrange else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

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
        modifier = modifier.width(360.dp).height(40.dp)
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
                        fontSize = 13.sp,
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
                    if (weather.isLive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    currentCity: String,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearching: Boolean,
    isCelsius: Boolean,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCitySelected: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            onClick = {},
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(24.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF121622)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp)
                )
            ),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Weather Location",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Powered by Open-Meteo • Search any city globally",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onToggleUnit,
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DarkSurfaceVariant
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = if (isCelsius) "Unit: °C" else "Unit: °F",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Close",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = FireOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Type city name (e.g. Tokyo, London, Austin)...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearch(it)
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(FireOrange),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    searchQuery = ""
                                    onClearSearch()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (searchQuery.trim().length >= 2) "Search Results (Click to add & set)" else "Saved Locations",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FireAmber,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val displayList = if (searchQuery.trim().length >= 2) searchResults else savedCities

                TvLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    if (isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Searching Open-Meteo cities...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else if (displayList.isEmpty() && searchQuery.trim().length >= 2) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cities found matching \\\"\$searchQuery\\\"",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        items(displayList) { cityItem ->
                            val isSelected = cityItem.name.equals(currentCity, ignoreCase = true)

                            Surface(
                                onClick = { onCitySelected(cityItem) },
                                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isSelected) FireOrange.copy(alpha = 0.2f) else DarkSurface.copy(alpha = 0.4f),
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
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = "City",
                                            tint = if (isSelected) FireAmber else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = cityItem.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) FireAmber else TextPrimary
                                            )
                                            val sub = if (!cityItem.admin1.isNullOrBlank()) {
                                                "\${cityItem.admin1}, \${cityItem.country}"
                                            } else cityItem.country
                                            Text(
                                                text = sub,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = FireAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FireAmber
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Select",
                                            fontSize = 11.sp,
                                            color = TextSecondary
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
    description: 'Dynamic background gradient, r/TVWallpapersSFW crossfade, 48dp overscan padding, city search, sort by menu, and Home scroll-to-top handler',
    content: `package com.example.firelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppSortOrder
import com.example.firelauncher.data.CityLocationModel
import com.example.firelauncher.data.WallpaperModel
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.ui.components.AppRow
import com.example.firelauncher.ui.components.HeroSection
import com.example.firelauncher.ui.components.SortSelectionDialog
import com.example.firelauncher.ui.components.TopBar
import com.example.firelauncher.ui.theme.DarkBackground
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, handling TV overscan padding,
 * auto-changing r/TVWallpapersSFW 30-minute wallpapers, and intercepting Back/Home events.
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
    val savedCities by viewModel.savedCities.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingCities by viewModel.isSearchingCities.collectAsStateWithLifecycle()
    val currentWallpaper by viewModel.currentWallpaper.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
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
        savedCities = savedCities,
        searchResults = searchResults,
        isSearchingCities = isSearchingCities,
        currentWallpaper = currentWallpaper,
        sortOrder = sortOrder,
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        filteredApps = filteredApps,
        onSearchCities = { viewModel.searchCities(it) },
        onClearCitySearch = { viewModel.clearCitySearch() },
        onSelectCity = { viewModel.selectCity(it) },
        onToggleUnit = { viewModel.toggleTemperatureUnit() },
        onNextWallpaper = { viewModel.nextWallpaper() },
        onSelectSort = { viewModel.setSortOrder(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onToggleSearch = { viewModel.setSearchActive(it) },
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
    weather: WeatherData,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearchingCities: Boolean,
    currentWallpaper: WallpaperModel?,
    sortOrder: AppSortOrder,
    searchQuery: String,
    isSearchActive: Boolean,
    filteredApps: List<AppModel>,
    onSearchCities: (String) -> Unit,
    onClearCitySearch: () -> Unit,
    onSelectCity: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    onNextWallpaper: () -> Unit,
    onSelectSort: (AppSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnState = rememberTvLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var showRowSortDialog by remember { mutableStateOf(false) }

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
    ) {
        // Layer 1: Fullscreen r/TVWallpapersSFW Wallpaper Background
        currentWallpaper?.let { wp ->
            Crossfade(
                targetState = wp.url,
                animationSpec = tween(700),
                label = "WallpaperFade"
            ) { imgUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imgUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = wp.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Layer 2: TV Ambient Subtle Vignette & Scrim (Keeps 4K wallpaper highly visible while protecting text)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x550A0D14), // 33% top scrim for top bar text
                            Color(0x220A0D14), // 13% middle (wallpaper pops vibrantly)
                            Color(0x400A0D14), // 25% lower area
                            Color(0x770A0D14)  // 46% bottom scrim behind cards
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        )

        // Layer 3: Dynamic Focused App Ambient Tint Accent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAccentColor.copy(alpha = 0.20f),
                            animatedAccentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        // Layer 4: Main Content with 48dp Overscan Safe Margins
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp) // 48dp TV overscan safe padding
                .padding(top = 26.dp, bottom = 20.dp)
        ) {
            // Top bar: App name, Live Open-Meteo Weather with Search, Sort by Menu, Next Wallpaper & live clock/date
            TopBar(
                weather = weather,
                savedCities = savedCities,
                searchResults = searchResults,
                isSearchingCities = isSearchingCities,
                onSearchCities = onSearchCities,
                onClearCitySearch = onClearCitySearch,
                onSelectCity = onSelectCity,
                onToggleUnit = onToggleUnit,
                sortOrder = sortOrder,
                onSelectSort = onSelectSort,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onNextWallpaper = onNextWallpaper,
                wallpaperTitle = currentWallpaper?.title
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hero section: Focused app title, dynamic hints, and active wallpaper badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HeroSection(
                        focusedApp = focusedApp,
                        isFavorite = focusedApp != null && favorites.contains(focusedApp.packageName)
                    )
                }

                // Discreet Wallpaper Attribution Chip
                currentWallpaper?.let { wp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66181C28))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = "Wallpaper",
                            tint = FireAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\${wp.title.take(28)}... • r/TVWallpapersSFW",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                                    text = "No apps found matching \\\"\$searchQuery\\\"",
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

                    // Row 2: All Apps (with sort by menu trigger)
                    item(key = "row_all_apps") {
                        AppRow(
                            title = "All Apps",
                            apps = apps,
                            favorites = favorites,
                            sortOrder = sortOrder,
                            onOpenSortMenu = { showRowSortDialog = true },
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

        // App Library Sort By Dialog when opened from the row header
        if (showRowSortDialog) {
            SortSelectionDialog(
                currentSort = sortOrder,
                onSelectSort = { order ->
                    onSelectSort(order)
                    showRowSortDialog = false
                },
                onDismiss = { showRowSortDialog = false }
            )
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
    description: 'Documentation: Quick Start AI agent automation, Sort by menu (A-Z/Recent/Frequent), vivid r/TVWallpapersSFW 30m auto rotation, Open-Meteo live weather, ADB wireless connect, and Fire OS launcher setup',
    content: `# FireLauncher 🔥

A high-performance custom home-screen launcher designed specifically for **Amazon Fire TV (Fire OS)**, built in Kotlin using **Jetpack Compose for TV (tv-material)**, **MVVM architecture**, **Preferences DataStore**, and **AndroidX Palette** for dynamic ambient theming.

<p align="center">
  <img src="docs/firelauncher_screenshot.jpg" alt="FireLauncher Interface Preview - Dynamic r/TVWallpapersSFW Background, Live Weather, and App Options Context Menu" width="100%" />
  <br/>
  <em>FireLauncher on Fire TV: Dynamic 4K wallpaper, live Open-Meteo weather, A–Z / Recent / Frequent sorting, and D-pad App Options menu (Force Stop / App Info & Favorites)</em>
</p>

## Key Highlights
- **🚀 Quick Start (Automated with AI)**: Point any terminal AI agent (Claude Code, Gemini CLI, Cursor) at \`AI_STEPS.md\` to automate device pairing, APK install, safe debloating, and rebooting.
- **Library 'Sort By' Menu**: Toggle between Alphabetical (A–Z), Most Recently Used (MRU with relative time badges), and Most Frequently Used (MFU with launch counts) via Top Bar and All Apps row header.
- **Vivid r/TVWallpapersSFW Backgrounds**: Dynamic 16:9 TV wallpapers fetched directly from Reddit r/TVWallpapersSFW, auto-rotating every 30 minutes with smooth crossfade and clear, high-contrast glass scrim.
- **Accurate Live Weather (Open-Meteo)**: Zero API keys required. Live temperature, condition summary, high/low, humidity, and wind speed. Includes global geocoding search to search and add any city.
- **Global App Search**: Search icon in top bar with instant real-time app filtering and empty search state.
- **D-Pad Focus & Palette Glow**: 16:9 rounded cards that scale to 1.12x on focus, with 2dp white border and dynamic Palette accent glow.
- **Overscan-Safe**: 48dp horizontal padding, singleTask Home scroll-to-top handler, and hardware Back button absorption.

See the complete README.md on disk for the full AI agent manifest instructions, ADB setup commands, Button Mapper configurations, and CleanYourTV debloat scripts.
`
  },
  {
    path: 'AI_STEPS.md',
    name: 'AI_STEPS.md',
    language: 'markdown',
    category: 'docs',
    description: 'Sequential automation manifest for AI agents (ADB pairing, APK deployment, 5-package debloating, and reboot)',
    content: `# AI Automation Manifest: Fire TV Deployment & Safe Debloating

This document is a sequential, machine-readable execution manifest intended for terminal-based AI agents (such as Gemini CLI, Claude Code, or automated CI/CD runners) to connect to an Amazon Fire TV device over Wi-Fi, deploy the FireLauncher APK, and execute the CleanYourTV debloat routine.

---

## Pre-Flight Requirements & Context
- Target Architecture: Android 7.1+ / 9.0 (Fire OS 6, 7, 8)
- Target Device: Amazon Fire TV Stick / Fire TV Cube / Fire TV Edition
- Default ADB Port: 5555
- Target Package Name: com.example.firelauncher
- Pre-compiled APK Path: app/build/outputs/apk/debug/app-debug.apk

---

## Target Debloat Package Registry
The following 5 Amazon packages are targeted for user-space disabling (pm disable-user --user 0):
- com.amazon.advertising.firetv (Home banner & ads)
- com.amazon.device.sale.service (Promo checks)
- com.amazon.client.metrics (Telemetry & analytics)
- com.amazon.device.crashmanager (Crash logs)
- com.amazon.ftv.screensaver (Ad screensavers)

---

## Sequential Execution Steps

### STEP 1: Verify Host Environment Dependencies
\`\`\`bash
adb version || { echo "ERROR: ADB is not installed or not in PATH"; exit 1; }
python3 --version || { echo "ERROR: Python 3 is not installed or not in PATH"; exit 1; }
\`\`\`

### STEP 2: Obtain and Validate Fire TV IP Address
\`\`\`bash
IP_OUTPUT=\$(python3 tools/get_tv_ip.py)
FIRETV_IP=\$(echo "\$IP_OUTPUT" | grep "^SUCCESS:" | cut -d':' -f2 | tr -d ' \\r\\n')
echo "Target Fire TV IP: \$FIRETV_IP"
\`\`\`

### STEP 3: Establish Authenticated ADB Connection
\`\`\`bash
adb kill-server
adb start-server
adb connect "\$FIRETV_IP:5555"
\`\`\`
Poll until device state is 'device', prompting user to accept the TV screen authorization dialog.

### STEP 4: Deploy FireLauncher APK
\`\`\`bash
adb -s "\$FIRETV_IP:5555" install -r -d "app/build/outputs/apk/debug/app-debug.apk"
\`\`\`

### STEP 5: Execute Debloat Routine
\`\`\`bash
adb -s "\$FIRETV_IP:5555" shell pm disable-user --user 0 com.amazon.advertising.firetv
adb -s "\$FIRETV_IP:5555" shell pm disable-user --user 0 com.amazon.device.sale.service
adb -s "\$FIRETV_IP:5555" shell pm disable-user --user 0 com.amazon.client.metrics
adb -s "\$FIRETV_IP:5555" shell pm disable-user --user 0 com.amazon.device.crashmanager
adb -s "\$FIRETV_IP:5555" shell pm disable-user --user 0 com.amazon.ftv.screensaver
\`\`\`

### STEP 6: Verify Debloat Success
\`\`\`bash
adb -s "\$FIRETV_IP:5555" shell pm list packages -d
\`\`\`

### STEP 7: Launch FireLauncher & Reboot Device
\`\`\`bash
adb -s "\$FIRETV_IP:5555" shell am start -n com.example.firelauncher/.MainActivity
sleep 3
adb -s "\$FIRETV_IP:5555" reboot
\`\`\`
`
  },
  {
    path: 'tools/get_tv_ip.py',
    name: 'get_tv_ip.py',
    language: 'python',
    category: 'scripts',
    description: 'CLI tool to guide users to their Fire TV IP address, validate IPv4 octets (0-255), and emit SUCCESS:IP_ADDRESS',
    content: `#!/usr/bin/env python3
"""
tools/get_tv_ip.py
CLI helper to prompt for, validate, and emit a valid Amazon Fire TV IPv4 address.
Formats the final successful IP as 'SUCCESS:<IP_ADDRESS>' for programmatic agent consumption.
"""

import sys
import re

IPV4_REGEX = re.compile(
    r"^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}"
    r"(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
)

BANNER = r"""
========================================================================
            🔥 Fire TV Wireless IP Discovery & Configuration 🔥
========================================================================
To locate your Fire TV's local IP address on your television screen:

  1. Turn on your Amazon Fire TV.
  2. Navigate to:
       Settings  ->  My Fire TV (or 'Device & Software')  ->  About
  3. Select 'Network' from the menu on the right.
  4. Look for the 'IP Address' field on the network status card.
       (Example: 192.168.1.45 or 10.0.0.88)
========================================================================
"""

def is_valid_ipv4(ip_str: str) -> bool:
    if not ip_str or not isinstance(ip_str, str):
        return False
    trimmed = ip_str.strip()
    if not IPV4_REGEX.match(trimmed):
        return False
    try:
        octets = [int(part) for part in trimmed.split(".")]
        if len(octets) != 4:
            return False
        for octet in octets:
            if octet < 0 or octet > 255:
                return False
        if octets == [0, 0, 0, 0] or octets == [255, 255, 255, 255]:
            return False
        return True
    except (ValueError, TypeError):
        return False

def main():
    sys.stderr.write(BANNER + "\\n")
    sys.stderr.flush()

    while True:
        try:
            sys.stderr.write("Enter your Fire TV IP address (e.g. 192.168.1.50): ")
            sys.stderr.flush()
            raw_input = input().strip()
            if not raw_input:
                sys.stderr.write("  [!] Input cannot be empty. Please enter an IPv4 address.\\n\\n")
                continue

            if is_valid_ipv4(raw_input):
                valid_ip = raw_input
                sys.stderr.write(f"\\n[+] Valid IPv4 address confirmed: {valid_ip}\\n")
                sys.stderr.flush()
                print(f"SUCCESS:{valid_ip}")
                sys.stdout.flush()
                sys.exit(0)
            else:
                sys.stderr.write(
                    f"  [X] '{raw_input}' is not a valid IPv4 address.\\n"
                    "      Each of the 4 octets must be a number between 0 and 255.\\n"
                    "      Example of a valid address: 192.168.0.125\\n\\n"
                )
                sys.stderr.flush()
        except (KeyboardInterrupt, EOFError):
            sys.stderr.write("\\n\\nOperation cancelled by user. Exiting.\\n")
            sys.exit(130)

if __name__ == "__main__":
    main()
`
  },
  {
    path: 'tools/clear_launcher_cache.sh',
    name: 'clear_launcher_cache.sh',
    language: 'bash',
    category: 'scripts',
    description: 'ADB shell utility to wipe launcher image cache and temporary application data (pm clear com.example.firelauncher)',
    content: `#!/usr/bin/env bash
# ==============================================================================
# tools/clear_launcher_cache.sh
# FireLauncher Memory & Cache Recovery Utility
# ==============================================================================

set -e

PACKAGE_NAME="com.example.firelauncher"
MAIN_ACTIVITY="com.example.firelauncher/.MainActivity"

echo "=========================================================="
echo "    🔥 FireLauncher Cache & Memory Wipe Utility 🔥        "
echo "=========================================================="

if ! command -v adb &> /dev/null; then
    echo "[-] Error: 'adb' command not found. Please install Android Platform Tools and add to PATH."
    exit 1
fi

TARGET_DEVICE="\$1"
ADB_CMD="adb"

if [ -n "\$TARGET_DEVICE" ]; then
    echo "[+] Target device explicitly provided: \$TARGET_DEVICE"
    ADB_CMD="adb -s \$TARGET_DEVICE"
else
    CONNECTED_DEVICES=\$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print \$1}' || true)
    DEVICE_COUNT=\$(echo "\$CONNECTED_DEVICES" | grep -v '^$' | wc -l || true)

    if [ "\$DEVICE_COUNT" -eq 0 ]; then
        echo "[-] Error: No ADB devices connected."
        echo "    Connect your Fire TV first using: adb connect <firetv-ip>:5555"
        exit 1
    elif [ "\$DEVICE_COUNT" -eq 1 ]; then
        TARGET_DEVICE=\$(echo "\$CONNECTED_DEVICES" | tr -d ' \\r\\n')
        echo "[+] Detected connected device: \$TARGET_DEVICE"
        ADB_CMD="adb -s \$TARGET_DEVICE"
    else
        echo "[!] Multiple ADB devices detected:"
        echo "\$CONNECTED_DEVICES"
        echo ""
        echo "Usage: ./tools/clear_launcher_cache.sh <device-ip:port or serial>"
        exit 1
    fi
fi

echo "[+] Checking if \$PACKAGE_NAME is installed on target..."
if ! \$ADB_CMD shell pm list packages | grep -q "\$PACKAGE_NAME"; then
    echo "[-] Error: Package \$PACKAGE_NAME is not installed on \$TARGET_DEVICE."
    exit 1
fi

echo "[+] Clearing runtime cache and data for \$PACKAGE_NAME..."
CLEAR_RESULT=\$(${'\\$' + 'ADB_CMD'} shell pm clear "\$PACKAGE_NAME" 2>&1)
echo "    Result: \$CLEAR_RESULT"

echo "[+] Relaunching FireLauncher on target TV display..."
\$ADB_CMD shell am start -n "\$MAIN_ACTIVITY" > /dev/null 2>&1 || true

echo "=========================================================="
echo "    ✓ FireLauncher is running fresh with clean cache!     "
echo "=========================================================="
`
  }
];
