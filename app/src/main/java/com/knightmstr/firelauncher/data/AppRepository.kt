package com.knightmstr.firelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for scanning installed applications and system shortcuts,
 * sorting them, loading icons/banners, and launching them with explicit intents.
 */
class AppRepository(private val context: Context) {

    companion object {
        /** Key of the home-screen tile that opens the All Apps grid; handled by the ViewModel. */
        const val ALL_APPS_KEY = "system.all_apps"

        // Fire OS's own settings screens, opened by name. The standard Settings action goes through
        // Amazon's home screen first on Fire OS, so you'd see Amazon's home instead of just Settings.
        // Approach from couch-tv (github.com/Seagey/couch-tv, MIT).
        private val FIRE_SETTINGS =
            ComponentName("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity")
        private val FIRE_NETWORK =
            ComponentName("com.amazon.tv.settings.v2", "com.amazon.tv.settings.v2.tv.network.NetworkActivity")
    }

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

    /** Tile at the start of the home row that opens the full All Apps grid. */
    fun getAllAppsTile(): AppModel = AppModel(
        packageName = ALL_APPS_KEY,
        activityName = "FireLauncher.AllApps",
        label = "All Apps",
        icon = ColorDrawable(Color.TRANSPARENT),
        accentColor = 0xFFFF9800.toInt(), // Fire amber
        isSystemShortcut = true,
        systemIntentAction = ALL_APPS_KEY
    )

    /** Fire TV Settings main menu on its own (not Amazon's home), else Android's Settings. */
    fun openFireTvSettings(): Boolean =
        openFirstAvailable(Intent().setComponent(FIRE_SETTINGS), Intent(Settings.ACTION_SETTINGS))

    /** Fire TV Network (Wi-Fi) screen, else Android's Wi-Fi settings. */
    fun openNetworkSettings(): Boolean =
        openFirstAvailable(Intent().setComponent(FIRE_NETWORK), Intent(Settings.ACTION_WIFI_SETTINGS))

    /**
     * Starts the first intent this TV can open. Screens named by component must be exported (public).
     * CLEAR_TASK makes Settings open on the requested page, not whichever page was left open last.
     */
    private fun openFirstAvailable(vararg candidates: Intent): Boolean {
        for (candidate in candidates) {
            val info = packageManager.resolveActivity(candidate, 0)?.activityInfo ?: continue
            if (candidate.component != null && !info.exported) continue
            val intent = Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    /**
     * Launches an app using an explicit ComponentName and FLAG_ACTIVITY_NEW_TASK.
     */
    fun launchApp(app: AppModel): Boolean {
        if (app.isSystemShortcut) return false // in-launcher tiles are handled by the ViewModel
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.activityName)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
            true
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
