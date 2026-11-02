package com.goal.ieltsreading.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records the learner's voice to a small file so they can play it back and compare
 * it with the model. Needs the RECORD_AUDIO permission (the screen asks for it).
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val file: File get() = File(context.cacheDir, "my_voice.m4a")

    val isRecording: Boolean get() = recorder != null
    val hasRecording: Boolean get() = file.exists() && file.length() > 0

    fun start(): Boolean {
        stopPlayback()
        return try {
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (e: Exception) {
            recorder = null
            false
        }
    }

    fun stop() {
        val r = recorder ?: return
        recorder = null
        try {
            r.stop()
        } catch (e: RuntimeException) {
            // Stopped too quickly: nothing was recorded.
            file.delete()
        } finally {
            r.release()
        }
    }

    fun play() {
        if (!hasRecording) return
        stopPlayback()
        val p = MediaPlayer()
        try {
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener { stopPlayback() }
            p.prepare()
            p.start()
            player = p
        } catch (e: Exception) {
            // A broken or empty recording: just don't play it.
            p.release()
        }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
    }

    /** Deletes the last recording (e.g. when the next phrase starts). */
    fun clear() {
        stopPlayback()
        file.delete()
    }

    fun release() {
        stop()
        stopPlayback()
    }
}
