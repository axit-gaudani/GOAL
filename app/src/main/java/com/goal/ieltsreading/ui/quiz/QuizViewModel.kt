package com.goal.ieltsreading.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.QuestionEntity
import com.goal.ieltsreading.domain.AnswerChecker
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class QuizState(
    val question: QuestionEntity,
    val index: Int,
    val total: Int,
    /** Answer chosen or typed by the student. */
    val answer: String?,
    /** True after "Check" was pressed; feedback is shown. */
    val checked: Boolean,
    val correct: Boolean
)

data class QuizFinished(val correct: Int, val total: Int)

/** Lesson practice: one question at a time with instant feedback and explanation. */
class QuizViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val lessonId: String = handle["lessonId"] ?: ""

    private var questions: List<QuestionEntity> = emptyList()
    private var index = 0
    private var score = 0
    private var answer: String? = null
    private var checked = false
    private var lastCorrect = false

    private val _lesson = MutableLiveData<LessonEntity?>()
    val lesson: LiveData<LessonEntity?> = _lesson
    private val _state = MutableLiveData<QuizState?>()
    val state: LiveData<QuizState?> = _state
    private val _finished = MutableLiveData<QuizFinished?>()
    val finished: LiveData<QuizFinished?> = _finished

    val showGujarati: Boolean get() = repository.prefs.showGujarati

    init {
        viewModelScope.launch {
            _lesson.value = repository.lesson(lessonId)
            questions = repository.questions(lessonId)
            if (questions.isEmpty()) finish() else publish()
        }
    }

    fun select(option: String) {
        if (checked) return
        answer = option
        publish()
    }

    fun check(typedAnswer: String? = null) {
        if (checked) return
        if (typedAnswer != null) answer = typedAnswer
        if (answer.isNullOrBlank()) return
        lastCorrect = AnswerChecker.isCorrect(questions[index], answer)
        if (lastCorrect) score++
        checked = true
        publish()
    }

    fun next() {
        if (!checked) return
        if (index + 1 >= questions.size) {
            finish()
        } else {
            index++
            answer = null
            checked = false
            publish()
        }
    }

    private fun finish() {
        val total = questions.size
        viewModelScope.launch {
            repository.saveLessonResult(lessonId, score, total)
            _finished.value = QuizFinished(score, total)
        }
    }

    fun finishedHandled() { _finished.value = null }

    private fun publish() {
        _state.value = QuizState(questions[index], index, questions.size, answer, checked, lastCorrect)
    }
}
