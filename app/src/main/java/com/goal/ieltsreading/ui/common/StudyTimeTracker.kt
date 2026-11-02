package com.goal.ieltsreading.ui.common

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.goal.ieltsreading.IeltsApp
import kotlinx.coroutines.launch

/**
 * Counts how long a learning screen is visible and adds it to today's study time.
 * Usage in a fragment: viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
 */
class StudyTimeTracker(private val app: IeltsApp) : DefaultLifecycleObserver {
    private var startedAt = 0L

    override fun onResume(owner: LifecycleOwner) {
        startedAt = SystemClock.elapsedRealtime()
    }

    override fun onPause(owner: LifecycleOwner) {
        if (startedAt == 0L) return
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000
        startedAt = 0L
        app.appScope.launch { app.repository.addStudySeconds(seconds) }
    }
}
