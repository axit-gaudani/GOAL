package com.goal.ieltsreading.ui.sentence

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.repo.RoundItem
import com.goal.ieltsreading.data.repo.SentenceExercise
import com.goal.ieltsreading.domain.SentenceChecker
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class RoundState(
    val item: RoundItem,
    val index: Int,
    val total: Int,
    /** Word tiles still available (BUILD). */
    val bank: List<Tile>,
    /** Word tiles placed in the answer (BUILD). */
    val answer: List<Tile>,
    /** FIX: index of the word the learner tapped as wrong, or -1. */
    val tappedWrong: Int,
    /** FILL/FIX: option chosen. */
    val chosen: String?,
    val checked: Boolean,
    val correct: Boolean,
    /** What the learner answered, shown in the result list when wrong. */
    val given: String
)

/** A word tile; [id] keeps duplicates ("the", "the") apart. */
data class Tile(val id: Int, val text: String)

/** [focus] is the word the sentence practises; the result list shows it in green. */
data class Missed(val en: String, val gu: String, val given: String, val tipGu: String, val focus: String = "")

data class RoundResult(val correct: Int, val total: Int, val seconds: Int, val missed: List<Missed>, val streak: Int = 0)

class SentenceRoundViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val mode: SentenceExercise? = (handle.get<String>("mode") ?: "").let { m ->
        SentenceExercise.entries.firstOrNull { it.name == m }
    }

    private var items: List<RoundItem> = emptyList()
    private var index = 0
    private var score = 0
    private val missed = mutableListOf<Missed>()
    private var startedAt = System.currentTimeMillis()

    private var bank = listOf<Tile>()
    private var answer = listOf<Tile>()
    private var tappedWrong = -1
    private var chosen: String? = null
    private var checked = false
    private var correct = false
    private var given = ""

    private val _state = MutableLiveData<RoundState?>()
    val state: LiveData<RoundState?> = _state
    private val _result = MutableLiveData<RoundResult?>()
    val result: LiveData<RoundResult?> = _result

    init { newRound() }

    fun newRound() = viewModelScope.launch {
        items = repository.sentenceRound(mode)
        index = 0
        score = 0
        missed.clear()
        startedAt = System.currentTimeMillis()
        _result.value = null
        if (items.isEmpty()) _result.value = RoundResult(0, 0, 0, emptyList()) else prepare()
    }

    private fun prepare() {
        val item = items[index]
        val words = SentenceChecker.tiles(item.sentence.en)
        // Two extra words from other sentences make building a little harder.
        val extras = items.filter { it !== item }.flatMap { SentenceChecker.tiles(it.sentence.en) }
            .filter { w -> words.none { it.equals(w, ignoreCase = true) } }.shuffled().take(2)
        bank = (words + extras).mapIndexed { i, w -> Tile(i, w) }.shuffled()
        answer = emptyList()
        tappedWrong = -1
        chosen = null
        checked = false
        correct = false
        given = ""
        publish()
    }

    fun tapBank(tile: Tile) { if (checked) return; bank = bank - tile; answer = answer + tile; publish() }
    fun tapAnswer(tile: Tile) { if (checked) return; answer = answer - tile; bank = bank + tile; publish() }
    fun tapWord(position: Int) { if (checked) return; tappedWrong = position; chosen = null; publish() }
    fun choose(option: String) { if (checked) return; chosen = option; publish() }

    /** Words of the sentence as shown for FIX: the focus word replaced by the mistake. */
    fun fixWords(item: RoundItem): List<String> = fillWords(item, item.sentence.focusWrong)

    /** Words of the sentence with the focus word replaced by [replacement] (FILL shows "____"). */
    fun fillWords(item: RoundItem, replacement: String): List<String> {
        val words = SentenceChecker.tiles(item.sentence.en).toMutableList()
        val i = SentenceChecker.focusIndex(item.sentence)
        if (i >= 0) words[i] = words[i].replace(item.sentence.focusWord, replacement)
        return words
    }

    fun canCheck(typed: String): Boolean = when (items.getOrNull(index)?.exercise) {
        SentenceExercise.BUILD -> answer.isNotEmpty()
        SentenceExercise.FILL -> chosen != null
        SentenceExercise.FIX -> tappedWrong >= 0 && chosen != null
        else -> typed.isNotBlank()
    }

    fun check(typed: String) {
        if (checked) return
        val item = items[index]
        val s = item.sentence
        val focus = SentenceChecker.focusIndex(s)
        when (item.exercise) {
            SentenceExercise.BUILD -> {
                given = answer.joinToString(" ") { it.text }
                correct = SentenceChecker.isCorrect(s, given)
            }
            SentenceExercise.FILL -> {
                given = fillWords(item, chosen ?: "____").joinToString(" ")
                correct = chosen == s.focusWord
            }
            SentenceExercise.FIX -> {
                given = fixWords(item).toMutableList().also {
                    if (tappedWrong in it.indices && chosen != null) it[tappedWrong] = chosen!!
                }.joinToString(" ")
                correct = tappedWrong == focus && chosen == s.focusWord
            }
            SentenceExercise.DICTATION, SentenceExercise.TRANSLATE -> {
                given = typed.trim()
                correct = SentenceChecker.isCorrect(s, given)
            }
        }
        checked = true
        if (correct) score++ else missed += Missed(s.en, s.gu, given, s.tipGu.ifBlank { s.tip }, s.focusWord)
        viewModelScope.launch { repository.recordSentence(s.id, correct) }
        publish()
    }

    fun next() {
        if (!checked) return
        if (index + 1 >= items.size) {
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            val total = items.size
            val finalScore = score
            val missedNow = missed.toList()
            _state.value = null
            viewModelScope.launch {
                val streak = runCatching { repository.streak() }.getOrDefault(0)
                _result.value = RoundResult(finalScore, total, seconds, missedNow, streak)
            }
        } else {
            index++
            prepare()
        }
    }

    private fun publish() {
        val item = items.getOrNull(index) ?: return
        _state.value = RoundState(item, index, items.size, bank, answer, tappedWrong, chosen, checked, correct, given)
    }
}
