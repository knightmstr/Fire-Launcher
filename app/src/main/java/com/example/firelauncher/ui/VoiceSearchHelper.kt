package com.example.firelauncher.ui

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

/**
 * Helper utility to construct and handle TV Remote Speech Recognition intents
 * using Android's standard speech recognizer contract.
 */
object VoiceSearchHelper {

    /**
     * Builds the standard Speech Recognition Intent used by Amazon Fire TV & Android TV remotes.
     */
    fun createSpeechIntent(context: Context, promptText: String = "Speak an app name to search..."): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Extra leanback support flag
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US"))
        }
    }

    /**
     * Extracts spoken query text from recognition intent result data.
     */
    fun extractSpokenQuery(data: Intent?): String? {
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return matches?.firstOrNull()?.trim()
    }
}
