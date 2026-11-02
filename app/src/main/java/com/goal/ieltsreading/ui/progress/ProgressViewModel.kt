package com.goal.ieltsreading.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.repo.OverallStats
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val _stats = MutableLiveData<OverallStats>()
    val stats: LiveData<OverallStats> = _stats
    val goalMinutes: Int get() = repository.prefs.dailyGoalMinutes

    fun load() = viewModelScope.launch { _stats.value = repository.overallStats() }
}
