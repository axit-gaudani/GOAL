package com.goal.ieltsreading.ui.speak

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Opens the phone's speech recognition screen and returns what it heard.
 * Create it as a Fragment property (before the fragment starts).
 */
class SpeechInput(fragment: Fragment, private val onResult: (String?) -> Unit) {

    private val launcher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        onResult(text)
    }

    private fun intent(prompt: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

    /** False on phones without a speech service; the screens then hide the check. */
    fun isAvailable(context: Context): Boolean =
        intent("").resolveActivity(context.packageManager) != null

    fun listen(prompt: String) {
        try {
            launcher.launch(intent(prompt))
        } catch (e: Exception) {
            onResult(null)
        }
    }
}
