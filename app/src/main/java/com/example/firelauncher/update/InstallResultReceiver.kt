package com.example.firelauncher.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.example.firelauncher.FireLauncherApp
import com.example.firelauncher.MainActivity

/** Receives PackageInstaller results, and relaunches FireLauncher after it updates itself. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as FireLauncherApp).updateRepository

        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            // Android wants the user to confirm: show its "Update this app?" screen
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { context.startActivity(it) }
            }
            // Usually never seen for a self-update: Android stops this process to replace it
            PackageInstaller.STATUS_SUCCESS -> repository.onInstallResult(true, null)
            PackageInstaller.STATUS_FAILURE_ABORTED -> repository.onInstallResult(false, "Update cancelled")
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_CONFLICT ->
                repository.onInstallResult(false, "Update is signed with a different key. Uninstall FireLauncher and install it manually once.")
            else -> repository.onInstallResult(
                false,
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed"
            )
        }
    }
}

/** Android sends MY_PACKAGE_REPLACED to the new version right after an update installs. */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // Best effort: newer Android versions may block background launches; the user can still open it.
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
