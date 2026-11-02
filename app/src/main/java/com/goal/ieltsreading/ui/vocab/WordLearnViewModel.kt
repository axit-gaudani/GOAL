package com.goal.ieltsreading.ui.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class WordLearnState(
    val word: VocabEntity?,
    val position: Int,
    val total: Int,
    val finished: Boolean,
    /** Title of the topic pack, shown above the card ("Environment · new word 2 of 5"). */
    val packTitle: String = ""
)

/** Shows today's 5 new words one by one. */
class WordLearnViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val packId: String = handle["packId"] ?: ""
    private var words: List<VocabEntity> = emptyList()
    private var index = 0
    private var packTitle = ""

    private val _state = MutableLiveData<WordLearnState>()
    val state: LiveData<WordLearnState> = _state

    val autoSpeak: Boolean get() = repository.prefs.autoSpeak

    init {
        viewModelScope.launch {
            words = repository.newWords(packId)
            packTitle = runCatching { repository.packs().firstOrNull { it.pack.id == packId }?.pack?.title }.getOrNull().orEmpty()
            publish()
        }
    }

    fun answer(alreadyKnown: Boolean) {
        val w = words.getOrNull(index) ?: return
        viewModelScope.launch { repository.learnWord(w.id, alreadyKnown) }
        index++
        publish()
    }

    private fun publish() {
        _state.value = WordLearnState(words.getOrNull(index), index + 1, words.size, index >= words.size, packTitle)
    }
}
