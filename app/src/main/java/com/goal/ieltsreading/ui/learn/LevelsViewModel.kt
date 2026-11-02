package com.goal.ieltsreading.ui.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.LevelEntity
import com.goal.ieltsreading.data.repo.LessonItem
import com.goal.ieltsreading.data.repo.LevelItem
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

class LevelsViewModel(app: Application) : AndroidViewModel(app) {
    private val _levels = MutableLiveData<List<LevelItem>>()
    val levels: LiveData<List<LevelItem>> = _levels

    fun load() = viewModelScope.launch { _levels.value = repository.levels() }
}

class LevelDetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val levelId: String = handle["levelId"] ?: ""

    private val _level = MutableLiveData<LevelEntity?>()
    val level: LiveData<LevelEntity?> = _level
    private val _lessons = MutableLiveData<List<LessonItem>>()
    val lessons: LiveData<List<LessonItem>> = _lessons

    fun load() = viewModelScope.launch {
        _level.value = repository.level(levelId)
        _lessons.value = repository.lessonsForLevel(levelId)
    }
}
