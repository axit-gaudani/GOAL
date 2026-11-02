package com.goal.ieltsreading.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Reads English text aloud with the phone's built-in text-to-speech (works offline
 * when the phone has an English voice installed). One instance lives in IeltsApp.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private var ready = false
    private var pending: Pair<String, Boolean>? = null

    /** False when the phone has no English voice; screens can show a hint. */
    var available = true
        private set

    // Created last: if no speech engine exists, onInit() is called straight away from this
    // constructor, and the properties above must already be set by then.
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            available = false
            return
        }
        // Prefer an Indian English voice, then British, then US.
        val locales = listOf(Locale("en", "IN"), Locale.UK, Locale.US)
        available = locales.any { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }
        ready = available
        pending?.let { (text, slow) -> speak(text, slow) }
        pending = null
    }

    /** Speaks [text]; [slow] uses 0.75× speed for beginners. */
    fun speak(text: String, slow: Boolean = false) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text to slow
            return
        }
        tts.setSpeechRate(if (slow) 0.7f else 0.95f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
    }

    fun stop() {
        if (ready) tts.stop()
    }
}
