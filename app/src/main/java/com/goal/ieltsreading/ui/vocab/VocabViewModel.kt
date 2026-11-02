package com.goal.ieltsreading.ui.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.WordWithProgress
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class VocabSummary(val due: Int, val deck: Int, val mastered: Int)

class VocabViewModel(app: Application) : AndroidViewModel(app) {
    private var allWords: List<WordWithProgress> = emptyList()
    private var query = ""
    private var onlyDeck = false

    private val _words = MutableLiveData<List<WordWithProgress>>()
    val words: LiveData<List<WordWithProgress>> = _words
    private val _summary = MutableLiveData<VocabSummary>()
    val summary: LiveData<VocabSummary> = _summary

    fun load() = viewModelScope.launch {
        val repo = repository
        allWords = repo.availableWords()
        _summary.value = VocabSummary(repo.dueCount(), repo.deckSize(), repo.masteredCount())
        applyFilter()
    }

    fun search(text: String) { query = text.trim(); applyFilter() }

    fun showOnlyDeck(only: Boolean) { onlyDeck = only; applyFilter() }

    private fun applyFilter() {
        _words.value = allWords.filter { w ->
            (!onlyDeck || w.box != null) &&
                (query.isEmpty() || w.word.contains(query, ignoreCase = true) || w.meaningGu.contains(query))
        }
    }
}
