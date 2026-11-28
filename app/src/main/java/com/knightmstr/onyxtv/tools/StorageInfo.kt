package com.knightmstr.onyxtv.tools

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.knightmstr.onyxtv.home.SelfAdb

/** Free space on the Fire TV, and freeing some by clearing every app's cache. */
object StorageInfo {

    data class Space(val freeBytes: Long, val totalBytes: Long)

    sealed interface CleanResult {
        data class Freed(val bytes: Long, val space: Space) : CleanResult
        data object AdbOff : CleanResult
        data object NotAllowed : CleanResult
        data class Failed(val reason: String) : CleanResult
    }

    fun read(): Space {
        val stat = StatFs(Environment.getDataDirectory().path)
        return Space(stat.availableBytes, stat.totalBytes)
    }

    /** Clears all apps' caches (Android rebuilds them as needed). Logins and data stay. */
    suspend fun freeUpSpace(context: Context): CleanResult {
        val before = read().freeBytes
        return when (val r = SelfAdb.run(context, SelfAdb.Command.TRIM_CACHES)) {
            is SelfAdb.ShellResult.Ok -> {
                val after = read()
                CleanResult.Freed((after.freeBytes - before).coerceAtLeast(0), after)
            }
            SelfAdb.ShellResult.AdbOff -> CleanResult.AdbOff
            SelfAdb.ShellResult.NotAllowed -> CleanResult.NotAllowed
            is SelfAdb.ShellResult.Failed -> CleanResult.Failed(r.reason)
        }
    }

    /** "1.2 GB", "340 MB". */
    fun describe(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format(java.util.Locale.US, "%.1f GB", mb / 1024) else "${mb.toLong()} MB"
    }
}
