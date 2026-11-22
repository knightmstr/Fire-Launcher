package com.knightmstr.firelauncher.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Installs a downloaded APK with Android's PackageInstaller session API.
 *
 * Android always shows its own "Do you want to update this app?" screen; Onyx TV can't
 * install silently. The result comes back to [InstallResultReceiver].
 */
object UpdateInstaller {

    /** Android 8+ requires a per-app "Install unknown apps" permission; 7.1 uses the global setting. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /**
     * Opens the "Install unknown apps" screen for Onyx TV. Returns false if this Fire OS
     * build doesn't have it (then: Settings → My Fire TV → Developer options → Install unknown apps).
     */
    fun openInstallPermissionSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                    session.fsync(output)
                }
            }
            // The installer fills in result extras, so this PendingIntent must be mutable on 12+.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).setPackage(context.packageName),
                flags
            )
            session.commit(callback.intentSender)
        }
    }
}
