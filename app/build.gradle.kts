import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
    ?: keystoreProperties.getProperty("storeFile")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    ?: keystoreProperties.getProperty("storePassword")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    ?: keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    ?: keystoreProperties.getProperty("keyPassword")

val hasReleaseKeystore = !releaseKeystorePath.isNullOrBlank() &&
    rootProject.file(releaseKeystorePath).exists() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

val resolvedVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull()
    ?: System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: 1

val resolvedVersionName = (project.findProperty("versionName") as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: "1.0.0"

android {
    namespace = "com.knightmstr.onyxtv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.knightmstr.onyxtv"
        minSdk = 25
        targetSdk = 34
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        // GitHub "owner/repo" checked for updates (change this if you fork the project)
        buildConfigField("String", "UPDATE_REPO", "\"knightmstr/Onyx-TV\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
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
            // Sign release builds only with the real upload keystore. Never fall back to the
            // debug key: a debug-signed release carries the universally-known Android debug
            // certificate, which breaks the same-signature guarantee the in-app updater relies
            // on (a tampered APK could then replace Onyx TV). When no keystore is configured the
            // task-graph check below fails the build instead of producing a mis-signed APK.
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
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
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        warningsAsErrors = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Refuse to build a release without a signing keystore rather than shipping an unsigned or
// debug-signed APK. Only fires when a release task is actually requested, so debug builds
// (assembleDebug, unit tests) are unaffected and need no keystore.
gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any {
        it.name.contains("assembleRelease") || it.name.contains("bundleRelease")
    }
    if (buildingRelease && !hasReleaseKeystore) {
        throw GradleException(
            "Release signing keystore is not configured, refusing to sign a release with the " +
                "debug key. Set ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, " +
                "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD (or a keystore.properties file) " +
                "before building a release."
        )
    }
}

dependencies {
    // AndroidX Core & Activity Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.activity:activity-ktx:1.9.2")

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

    // Unit Testing
    testImplementation("junit:junit:4.13.2")

    // Debugging tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
