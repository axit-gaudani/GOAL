package com.goal.ieltsreading.ui.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.repo.PackItem
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

data class VocabHubState(
    val wordsInDeck: Int,
    val mastered: Int,
    val due: Int,
    val currentPack: PackItem?,
    val newToday: Int,
    val packs: List<PackItem>
)

class VocabHubViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableLiveData<VocabHubState>()
    val state: LiveData<VocabHubState> = _state

    fun load() = viewModelScope.launch {
        val repo = repository
        val packs = repo.packs()
        val current = repo.currentPack()
        _state.value = VocabHubState(
            wordsInDeck = repo.deckSize(),
            mastered = repo.masteredCount(),
            due = repo.dueCount(),
            currentPack = current,
            newToday = current?.let { repo.newWords(it.pack.id).size } ?: 0,
            packs = packs
        )
    }

    fun choosePack(packId: String) {
        repository.prefs.currentPackId = packId
        load()
    }
}
