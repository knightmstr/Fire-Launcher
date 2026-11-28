package com.knightmstr.onyxtv.tools

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.knightmstr.onyxtv.update.UpdateInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Installs an app the phone uploaded ("Set up from phone → Install an app").
 *
 * Fire OS always shows its own "Install?" screen: nothing is installed without someone pressing
 * Install on the TV. The uploaded file is copied into the install session and deleted straight
 * away; the installed app stays, like any other.
 */
object AppSideloader {

    enum class State { IDLE, WAITING, INSTALLED, CANCELLED, FAILED }

    data class Status(
        val state: State = State.IDLE,
        val label: String? = null,
        val packageName: String? = null,
        val versionName: String? = null,
        val isUpdate: Boolean = false,
        val message: String? = null,
        /** Increases with every install, so the phone can tell results apart. */
        val serial: Int = 0
    )

    sealed interface Start {
        data class Started(val status: Status) : Start
        data class Failed(val message: String) : Start
    }

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    fun workDir(context: Context): File = File(context.cacheDir, "sideload").apply { mkdirs() }

    /** Reads the APK, checks it can be installed here, and opens Fire OS's install screen. */
    fun start(context: Context, apk: File): Start {
        try {
            if (_status.value.state == State.WAITING) return Start.Failed("Another app is waiting to be installed on the TV.")
            if (!UpdateInstaller.canInstall(context)) {
                UpdateInstaller.openInstallPermissionSettings(context)
                return Start.Failed(
                    "Onyx TV isn't allowed to install apps yet. On the TV, allow it under Install unknown apps, then try again."
                )
            }
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageArchiveInfo(apk.path, 0)
                ?: return Start.Failed("That file isn't an app this TV can read.")
            val pkg = info.packageName
            if (pkg == context.packageName) {
                return Start.Failed("To update Onyx TV itself, use Launcher settings → About → App updates.")
            }
            val appInfo = info.applicationInfo
            val label = appInfo?.let {
                it.sourceDir = apk.path
                it.publicSourceDir = apk.path
                runCatching { it.loadLabel(pm).toString() }.getOrNull()
            }?.takeIf { it.isNotBlank() } ?: pkg
            val isUpdate = runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess

            val serial = _status.value.serial + 1
            val status = Status(State.WAITING, label.take(60), pkg, info.versionName?.take(30), isUpdate, null, serial)
            _status.value = status
            commit(context, apk, serial)
            return Start.Started(status)
        } catch (e: Exception) {
            _status.value = _status.value.copy(state = State.FAILED, message = "Couldn't start the install: ${e.message ?: "error"}")
            return Start.Failed("Couldn't start the install on the TV.")
        } finally {
            // The install session has its own copy now (or nothing will be installed)
            apk.delete()
        }
    }

    private fun commit(context: Context, apk: File, serial: Int) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
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
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + (serial % 1000),
                Intent(context, SideloadResultReceiver::class.java).setPackage(context.packageName),
                flags
            )
            session.commit(callback.intentSender)
        }
    }

    internal fun onResult(state: State, message: String?) {
        _status.value = _status.value.copy(state = state, message = message)
    }

    private const val REQUEST_BASE = 40_000
}

/** Results from Fire OS's installer for a sideloaded app. Not exported. */
class SideloadResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                val shown = confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) }.isSuccess } == true
                if (!shown) AppSideloader.onResult(AppSideloader.State.FAILED, "The TV couldn't show the install screen.")
            }
            PackageInstaller.STATUS_SUCCESS -> AppSideloader.onResult(AppSideloader.State.INSTALLED, null)
            PackageInstaller.STATUS_FAILURE_ABORTED -> AppSideloader.onResult(AppSideloader.State.CANCELLED, "Cancelled on the TV.")
            else -> AppSideloader.onResult(AppSideloader.State.FAILED, messageFor(code, intent))
        }
    }

    private fun messageFor(code: Int, intent: Intent): String = when (code) {
        PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
            if (code == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE) "This app doesn't work on this Fire TV."
            else "That app is already installed from a different source. Uninstall it first, then try again."
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough space on the TV."
        PackageInstaller.STATUS_FAILURE_INVALID -> "The file is damaged or isn't a complete app."
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Fire OS blocked this install."
        else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.take(120) ?: "The install failed."
    }

}
