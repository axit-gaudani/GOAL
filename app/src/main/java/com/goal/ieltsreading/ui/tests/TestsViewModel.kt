package com.goal.ieltsreading.ui.tests

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.QuestionEntity
import com.goal.ieltsreading.data.db.TestEntity
import com.goal.ieltsreading.data.repo.TestItem
import com.goal.ieltsreading.domain.AnswerChecker
import com.goal.ieltsreading.domain.BandCalculator
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TestsViewModel(app: Application) : AndroidViewModel(app) {
    private val _tests = MutableLiveData<List<TestItem>>()
    val tests: LiveData<List<TestItem>> = _tests

    fun load() = viewModelScope.launch { _tests.value = repository.tests() }
}

/** A timed IELTS-style test. No feedback until the student submits (like the real exam). */
class TestViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val testId: String = handle["testId"] ?: ""

    private val _test = MutableLiveData<TestEntity?>()
    val test: LiveData<TestEntity?> = _test
    private val _questions = MutableLiveData<List<QuestionEntity>>()
    val questions: LiveData<List<QuestionEntity>> = _questions
    private val _secondsLeft = MutableLiveData<Int>()
    val secondsLeft: LiveData<Int> = _secondsLeft
    private val _answeredCount = MutableLiveData(0)
    val answeredCount: LiveData<Int> = _answeredCount
    private val _submittedAttemptId = MutableLiveData<Long?>()
    val submittedAttemptId: LiveData<Long?> = _submittedAttemptId

    /** questionId -> answer */
    val answers = mutableMapOf<String, String>()
    private val startedAt = SystemClock.elapsedRealtime()
    private var timerJob: Job? = null
    private var submitting = false

    init {
        viewModelScope.launch {
            val t = repository.test(testId)
            _test.value = t
            _questions.value = repository.questions(testId)
            if (t != null) startTimer(t.minutes * 60)
        }
    }

    private fun startTimer(totalSeconds: Int) {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
                val left = (totalSeconds - elapsed).coerceAtLeast(0)
                _secondsLeft.value = left
                if (left == 0) { submit(); break }
                delay(1000)
            }
        }
    }

    fun setAnswer(questionId: String, answer: String) {
        if (answer.isBlank()) answers.remove(questionId) else answers[questionId] = answer.trim()
        _answeredCount.value = answers.size
    }

    fun submit() {
        if (submitting) return
        submitting = true
        timerJob?.cancel()
        val qs = _questions.value.orEmpty()
        val correct = qs.count { AnswerChecker.isCorrect(it, answers[it.id]) }
        val band = BandCalculator.bandFor(correct, qs.size)
        val duration = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
        viewModelScope.launch {
            val id = repository.saveTestAttempt(testId, correct, qs.size, band, duration, answers.toMap())
            _submittedAttemptId.value = id
        }
    }

    fun submittedHandled() { _submittedAttemptId.value = null }
}
