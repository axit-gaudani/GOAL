package com.goal.ieltsreading

import android.app.Application
import com.goal.ieltsreading.audio.Speaker
import com.goal.ieltsreading.data.content.ContentSeeder
import com.goal.ieltsreading.data.db.AppDatabase
import com.goal.ieltsreading.data.prefs.UserPrefs
import com.goal.ieltsreading.data.repo.LearningRepository
import com.goal.ieltsreading.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class = one object that lives as long as the app.
 * It creates the database and repository once (simple manual dependency injection).
 */
class IeltsApp : Application() {

    lateinit var prefs: UserPrefs
        private set
    lateinit var repository: LearningRepository
        private set

    /** Text-to-speech for words, sentences and phrases (created when first used). */
    val speaker: Speaker by lazy { Speaker(this) }

    /** For work that must finish even if the screen closes (e.g. saving study time). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        prefs = UserPrefs(this)
        val db = AppDatabase.create(this)
        repository = LearningRepository(this, db, ContentSeeder(this, db, prefs), prefs)
        ReminderScheduler.createChannel(this)
        // Load the lessons into the database in the background on first launch.
        appScope.launch { repository.todaySummary() }
    }
}
