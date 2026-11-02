package com.goal.ieltsreading.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.WordWithProgress
import com.goal.ieltsreading.data.repo.LessonItem
import com.goal.ieltsreading.data.repo.TodaySummary
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class HomeState(
    val userName: String,
    val today: TodaySummary,
    val nextLesson: LessonItem?,
    val nextLessonLevelTitle: String,
    val dueWords: Int,
    val wordOfDay: WordWithProgress?,
    val coursePercent: Int,
    val dueSentences: Int
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableLiveData<HomeState>()
    val state: LiveData<HomeState> = _state

    fun load() = viewModelScope.launch {
        val repo = repository
        val today = repo.todaySummary()
        val levels = repo.levels()
        val next = repo.nextLesson()
        val level = levels.firstOrNull { it.level.id == next?.lesson?.levelId }
        val total = levels.sumOf { it.totalLessons }
        val done = levels.sumOf { it.completedLessons }
        _state.value = HomeState(
            userName = repo.prefs.userName,
            today = today,
            nextLesson = next,
            nextLessonLevelTitle = level?.let { "Level ${it.level.sortOrder} · ${it.level.title}" }.orEmpty(),
            dueWords = repo.dueCount(),
            wordOfDay = repo.wordOfTheDay(),
            coursePercent = if (total == 0) 0 else done * 100 / total,
            dueSentences = repo.dueSentenceCount()
        )
    }
}
