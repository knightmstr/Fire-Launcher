package com.knightmstr.onyxtv.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.knightmstr.onyxtv.screensaver.ScreensaverTakeover

/**
 * Lets adb (or an AI agent following AI_STEPS.md) switch on the Fire TV extras without anyone
 * touching the remote, after `pm grant … WRITE_SECURE_SETTINGS`:
 *
 *   adb shell am broadcast -a com.knightmstr.onyxtv.SETUP \
 *     -n com.knightmstr.onyxtv/.home.AdbSetupReceiver \
 *     --ez home_button true --ez screensaver true --ez keep_awake true
 *
 * Each extra is optional; leave one out to keep it as it is. Same effect as the switches in
 * Launcher settings, so turning them off there still puts Amazon's settings back.
 *
 * Only adb can send this: the receiver requires android.permission.DUMP, which the adb shell has
 * and apps can't get. The result line reports the state afterwards, e.g.
 * `result=-1, data="granted=true home_button=true screensaver=true keep_awake=true"`.
 */
class AdbSetupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val granted = HomeWatch.canManage(context)
        if (granted) {
            if (intent.hasExtra(EXTRA_HOME)) {
                val want = intent.getBooleanExtra(EXTRA_HOME, false)
                if (want != HomeWatch.isEnabled(context)) HomeWatch.setWanted(context, want)
            }
            // Only switch when it changes: switching on again would save our own values as
            // "Amazon's", and turning it off later couldn't put Amazon's back
            if (intent.hasExtra(EXTRA_SCREENSAVER)) {
                val want = intent.getBooleanExtra(EXTRA_SCREENSAVER, false)
                if (want != ScreensaverTakeover.isActive(context)) ScreensaverTakeover.setOn(context, want)
            }
            if (intent.hasExtra(EXTRA_AWAKE)) {
                val want = intent.getBooleanExtra(EXTRA_AWAKE, false)
                if (want != ScreensaverTakeover.keepAwake(context)) ScreensaverTakeover.setKeepAwake(context, want)
            }
        }
        resultCode = if (granted) Activity.RESULT_OK else Activity.RESULT_CANCELED
        resultData = "granted=$granted" +
            " home_button=${HomeWatch.isEnabled(context)}" +
            " screensaver=${ScreensaverTakeover.isActive(context)}" +
            " keep_awake=${ScreensaverTakeover.keepAwake(context)}" +
            if (ScreensaverTakeover.needsRestart(context)) " restart_needed=true" else ""
    }

    companion object {
        const val ACTION = "com.knightmstr.onyxtv.SETUP"
        const val EXTRA_HOME = "home_button"
        const val EXTRA_SCREENSAVER = "screensaver"
        const val EXTRA_AWAKE = "keep_awake"
    }
}
