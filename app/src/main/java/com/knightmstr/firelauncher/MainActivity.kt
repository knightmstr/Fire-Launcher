package com.knightmstr.firelauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.knightmstr.firelauncher.data.LocalPhotoSource
import com.knightmstr.firelauncher.home.HomeWatch
import com.knightmstr.firelauncher.screensaver.ScreensaverTakeover
import com.knightmstr.firelauncher.ui.LauncherRoot
import com.knightmstr.firelauncher.ui.LauncherViewModel
import com.knightmstr.firelauncher.ui.VoiceSearchHelper
import com.knightmstr.firelauncher.ui.theme.FireLauncherTheme
import kotlinx.coroutines.launch

/**
 * Single-task main Activity acting as Leanback and Home launcher for Fire OS.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    // Activity result launcher for TV remote voice recognition
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val query = VoiceSearchHelper.extractSpokenQuery(result.data)
            if (!query.isNullOrBlank()) {
                viewModel.onVoiceResult(query)
            }
        }
    }

    private val photoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPhotoPermissionResult() }

    private fun launchVoiceSearch() {
        try {
            val intent = VoiceSearchHelper.createSpeechIntent(this)
            speechRecognitionLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice recognition service not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fire OS home screen launchers must absorb unmatched back presses rather than exiting
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Deliberately absorbed on home launcher
                }
            }
        )

        // Lock window to immersive TV fullscreen
        hideSystemUI()

        // Ask for photo access when the user taps "Allow photo access" in Launcher Settings
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoPermissionRequests.collect {
                    photoPermissionLauncher.launch(LocalPhotoSource.PHOTO_PERMISSION)
                }
            }
        }

        setContent {
            FireLauncherTheme {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isInFront = true
        hideSystemUI()
        // Put the Home button watcher back if Android dropped it (and the user wants it)
        HomeWatch.ensure(this)
        ScreensaverTakeover.ensure(this)
        // Refresh installed apps whenever launcher returns to foreground
        viewModel.refreshApps()
        viewModel.onForegroundChanged(true)
    }

    override fun onPause() {
        isInFront = false
        viewModel.onForegroundChanged(false)
        super.onPause()
    }

    /**
     * Every key press resets the idle timer. While the screensaver is showing, the key
     * that wakes it is swallowed so it doesn't also launch or move focus.
     *
     * RestrictedApi is a lint false positive: androidx.core's ComponentActivity also declares
     * dispatchKeyEvent, but this overrides the public android.app.Activity method.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.screensaverVisible.value) {
            if (event.action == KeyEvent.ACTION_UP) viewModel.onUserInteraction()
            return true
        }
        viewModel.onUserInteraction()
        return super.dispatchKeyEvent(event)
    }

    /**
     * When the user presses the Home key while Onyx TV is already running,
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == 231 /* KEYCODE_VOICE_ASSIST */) {
            // Intercept Fire TV Alexa remote microphone button press
            launchVoiceSearch()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Fire TV remote ☰ button opens the options menu for the focused card
            viewModel.openContextMenuForFocused()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun hideSystemUI() {
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

    companion object {
        /** True while Onyx TV is on screen (read by the Home button watcher). */
        @Volatile var isInFront = false
    }
}
