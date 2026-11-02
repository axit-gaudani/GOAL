package com.goal.ieltsreading.ui.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Match pairs

data class MatchCard(val wordId: String, val text: String, val english: Boolean)

data class MatchState(
    val left: List<MatchCard>,
    val right: List<MatchCard>,
    val selected: MatchCard?,
    val matchedIds: Set<String>,
    /** Last wrong pair, shown red for a moment. */
    val wrong: Pair<MatchCard, MatchCard>?,
    val mistakes: Int,
    val finished: Boolean,
    val seconds: Int,
    val lastMatched: VocabEntity?,
    val empty: Boolean
)

/** Match 5 English words with their Gujarati meanings. */
class MatchGameViewModel(app: Application) : AndroidViewModel(app) {
    private var words: List<VocabEntity> = emptyList()
    private var left = listOf<MatchCard>()
    private var right = listOf<MatchCard>()
    private var selected: MatchCard? = null
    private val matched = mutableSetOf<String>()
    private var wrong: Pair<MatchCard, MatchCard>? = null
    private var mistakes = 0
    private var startedAt = 0L
    private var lastMatched: VocabEntity? = null

    private val _state = MutableLiveData<MatchState>()
    val state: LiveData<MatchState> = _state

    init { newGame() }

    fun newGame() = viewModelScope.launch {
        words = repository.gameWords(5)
        left = words.map { MatchCard(it.id, it.word, true) }.shuffled()
        right = words.map { MatchCard(it.id, it.meaningGu, false) }.shuffled()
        selected = null; matched.clear(); wrong = null; mistakes = 0; lastMatched = null
        startedAt = System.currentTimeMillis()
        publish()
    }

    fun tap(card: MatchCard) {
        if (card.wordId in matched) return
        wrong = null
        val first = selected
        when {
            first == null || first.english == card.english -> selected = card
            first.wordId == card.wordId -> {
                matched += card.wordId
                selected = null
                lastMatched = words.firstOrNull { it.id == card.wordId }
                viewModelScope.launch { repository.reviewWord(card.wordId, knewIt = true) }
            }
            else -> {
                wrong = first to card
                mistakes++
                selected = null
            }
        }
        publish()
    }

    private fun publish() {
        _state.value = MatchState(
            left, right, selected, matched.toSet(), wrong, mistakes,
            finished = words.isNotEmpty() && matched.size == words.size,
            seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt(),
            lastMatched = lastMatched,
            empty = words.size < 2
        )
    }
}

// ---------------------------------------------------------------- Spelling

data class SpellingState(
    val word: VocabEntity?,
    val position: Int,
    val total: Int,
    val checked: Boolean,
    val correct: Boolean,
    val score: Int,
    val finished: Boolean
)

/** Hear a word and type it. */
class SpellingViewModel(app: Application) : AndroidViewModel(app) {
    private var words: List<VocabEntity> = emptyList()
    private var index = 0
    private var score = 0
    private var checked = false
    private var correct = false

    private val _state = MutableLiveData<SpellingState>()
    val state: LiveData<SpellingState> = _state

    init { restart() }

    fun restart() = viewModelScope.launch {
        words = repository.gameWords(10)
        index = 0; score = 0; checked = false; correct = false
        publish()
    }

    fun check(typed: String) {
        val w = words.getOrNull(index) ?: return
        if (checked || typed.isBlank()) return
        correct = typed.trim().equals(w.word, ignoreCase = true)
        if (correct) score++
        checked = true
        viewModelScope.launch { repository.reviewWord(w.id, correct) }
        publish()
    }

    fun next() {
        if (!checked) return
        index++
        checked = false
        correct = false
        publish()
    }

    private fun publish() {
        _state.value = SpellingState(
            words.getOrNull(index), index + 1, words.size, checked, correct, score,
            finished = words.isNotEmpty() && index >= words.size
        )
    }
}
