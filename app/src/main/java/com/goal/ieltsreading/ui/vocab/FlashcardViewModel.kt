package com.goal.ieltsreading.ui.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.WordWithProgress
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class FlashcardState(
    val word: WordWithProgress?,
    val revealed: Boolean,
    val position: Int,
    val total: Int,
    val known: Int,
    val unknown: Int,
    val finished: Boolean
)

/** Review session: words the student did not know come back once more at the end. */
class FlashcardViewModel(app: Application) : AndroidViewModel(app) {
    private val queue = ArrayDeque<WordWithProgress>()
    private val retried = mutableSetOf<String>()
    private var total = 0
    private var done = 0
    private var known = 0
    private var unknown = 0
    private var current: WordWithProgress? = null
    private var revealed = false

    private val _state = MutableLiveData<FlashcardState>()
    val state: LiveData<FlashcardState> = _state

    init {
        viewModelScope.launch {
            queue.addAll(repository.dueWords(limit = 20))
            total = queue.size
            next()
        }
    }

    fun reveal() { revealed = true; publish() }

    fun answer(knewIt: Boolean) {
        val word = current ?: return
        val firstTime = word.id !in retried
        if (firstTime) {
            // Only the first answer changes the spaced-repetition box.
            viewModelScope.launch { repository.reviewWord(word.id, knewIt) }
            if (knewIt) known++ else unknown++
        }
        if (!knewIt && firstTime) {
            retried += word.id
            queue.addLast(word)
            total++
        }
        done++
        next()
    }

    private fun next() {
        current = queue.removeFirstOrNull()
        revealed = false
        publish()
    }

    private fun publish() {
        _state.value = FlashcardState(current, revealed, done + 1, total, known, unknown, finished = current == null)
    }
}
