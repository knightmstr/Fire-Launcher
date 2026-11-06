package com.example.firelauncher.data

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
