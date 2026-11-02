package com.goal.ieltsreading.ui.sentence

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.repo.PatternMastery
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class SentenceHubState(val levelName: String, val due: Int, val patterns: List<PatternMastery>, val level: Int)

class SentenceHubViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableLiveData<SentenceHubState>()
    val state: LiveData<SentenceHubState> = _state

    fun load() = viewModelScope.launch {
        val repo = repository
        _state.value = SentenceHubState(
            levelName = repo.currentLevelName(),
            due = repo.dueSentenceCount(),
            patterns = repo.patternMastery(),
            level = if (repo.prefs.unlockAll) 6 else repo.highestUnlockedLevelOrder().coerceAtLeast(0)
        )
    }
}
