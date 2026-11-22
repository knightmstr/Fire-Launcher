package com.knightmstr.onyxtv.home

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.knightmstr.onyxtv.MainActivity

/**
 * The Home button watcher. It is told only when one of Amazon's launcher windows comes forward
 * (res/xml/home_watch.xml limits events to that package) and reads nothing on screen: when the
 * window is Amazon's home itself, Onyx TV is brought back on top.
 *
 * Based on couch-tv's FireWatch (github.com/Seagey/couch-tv, MIT).
 */
class HomeWatchService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastJumpAt = 0L
    private var amazonHomeLastSeen = false
    private var recheckPending = false

    private val recheck = Runnable {
        recheckPending = false
        // A quick second Home press inside the debounce: if Amazon's home is still the last thing
        // we saw and Onyx TV isn't in front, jump once more. One re-check, never a loop.
        if (amazonHomeLastSeen && !MainActivity.isInFront) bringBack()
    }

    override fun onServiceConnected() {
        HomeWatch.connected = true
        // Bound shortly after a restart: Amazon's home came up before we could see it, so put
        // Onyx TV in front once.
        if (SystemClock.elapsedRealtime() < BOOT_WINDOW_MS && !MainActivity.isInFront) bringBack()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        HomeWatch.connected = false
        handler.removeCallbacks(recheck)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        amazonHomeLastSeen = HomeWatch.isAmazonHome(this, event.packageName, event.className)
        if (!amazonHomeLastSeen || HomeWatch.amazonAllowedNow()) return

        val wait = DEBOUNCE_MS - (SystemClock.elapsedRealtime() - lastJumpAt)
        if (wait > 0) {
            if (!recheckPending) {
                recheckPending = true
                handler.postDelayed(recheck, wait + 50)
            }
            return
        }
        bringBack()
    }

    private fun bringBack() {
        lastJumpAt = SystemClock.elapsedRealtime()
        // Fire OS 7 (Android 9) holds activity starts for 5 s after a Home press; moving our
        // existing task to the front is not held, so it's much quicker there.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && moveOwnTaskToFront()) return
        runCatching {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun moveOwnTaskToFront(): Boolean = runCatching {
        val task = getSystemService(ActivityManager::class.java).appTasks.firstOrNull {
            it.taskInfo.baseActivity?.className == MainActivity::class.java.name
        } ?: return false
        task.moveToFront()
        true
    }.getOrDefault(false)

    private companion object {
        const val DEBOUNCE_MS = 1200L
        const val BOOT_WINDOW_MS = 2 * 60_000L
    }
}
