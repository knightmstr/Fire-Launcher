package com.example.firelauncher

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
