package com.goal.ieltsreading.ui.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.content.AbcLetter
import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

/** Shared by the lesson screen and its three tabs (Learn, Words, Read). */
class LessonViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val lessonId: String = handle["lessonId"] ?: ""

    private val _lesson = MutableLiveData<LessonEntity?>()
    val lesson: LiveData<LessonEntity?> = _lesson
    private val _words = MutableLiveData<List<VocabEntity>>()
    val words: LiveData<List<VocabEntity>> = _words
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    val showGujaratiByDefault: Boolean get() = repository.prefs.showGujarati

    /** Letters of an ABC lesson (Level −1); empty for all other lessons. */
    val letters: List<AbcLetter> = repository.abcLetters(lessonId)

    init {
        viewModelScope.launch {
            _lesson.value = repository.lesson(lessonId)
            _words.value = repository.vocabulary(lessonId)
        }
    }

    fun addAllWordsToDeck(doneMessage: String) = viewModelScope.launch {
        repository.addLessonWordsToDeck(lessonId)
        _message.value = doneMessage
    }

    fun messageShown() { _message.value = null }
}
