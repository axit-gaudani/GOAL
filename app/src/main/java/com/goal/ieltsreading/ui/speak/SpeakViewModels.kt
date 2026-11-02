package com.goal.ieltsreading.ui.speak

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goal.ieltsreading.data.content.ConversationOption
import com.goal.ieltsreading.data.content.SpeakingPhrase
import com.goal.ieltsreading.data.content.SpeakingUnit
import com.goal.ieltsreading.data.repo.SpeakingUnitItem
import com.goal.ieltsreading.domain.SpeechMatcher
import com.goal.ieltsreading.ui.common.repository
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Hub

data class SpeakHubState(val phrase: SpeakingPhrase?, val units: List<SpeakingUnitItem>)

class SpeakHubViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableLiveData<SpeakHubState>()
    val state: LiveData<SpeakHubState> = _state

    fun load() = viewModelScope.launch {
        _state.value = SpeakHubState(repository.phraseOfTheDay(), repository.speakingUnits())
    }
}

// ---------------------------------------------------------------- Say the phrase

data class PhraseState(
    val unitTitle: String,
    val phrase: SpeakingPhrase,
    val index: Int,
    val total: Int,
    /** Result of the speech check for this phrase, null when not checked yet. */
    val heard: String?,
    val match: SpeechMatcher.Result?,
    val clearCount: Int,
    val finished: Boolean
)

/** Step 1 of a speaking unit: listen to each phrase, say it, check it. */
class SpeakPhraseViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val unitId: String = handle["unitId"] ?: ""
    private val unit: SpeakingUnit? = repository.speakingUnit(unitId)
    private var index = 0
    private var heard: String? = null
    private var match: SpeechMatcher.Result? = null
    private val clear = mutableSetOf<Int>()
    private val practised = mutableSetOf<Int>()

    /** False when the phone has no speech recognition; recording + listening still works. */
    var checkAvailable = true

    private val _state = MutableLiveData<PhraseState?>()
    val state: LiveData<PhraseState?> = _state

    init { publish() }

    fun onHeard(text: String) {
        val phrase = unit?.phrases?.getOrNull(index) ?: return
        heard = text
        match = SpeechMatcher.compare(phrase.en, text)
        practised += index
        if (match?.clear == true) clear += index
        publish()
    }

    fun onRecorded() {
        practised += index
        publish()
    }

    fun next() {
        val total = unit?.phrases?.size ?: 0
        index++
        heard = null
        match = null
        if (index >= total) {
            viewModelScope.launch {
                repository.savePhraseStep(unitId, clear.size, practised.size, total, checkAvailable)
            }
        }
        publish()
    }

    private fun publish() {
        val u = unit ?: return
        val total = u.phrases.size
        val finished = index >= total
        val phrase = u.phrases.getOrNull(index.coerceAtMost(total - 1)) ?: return
        _state.value = PhraseState(u.title, phrase, index, total, heard, match, clear.size, finished)
    }
}

// ---------------------------------------------------------------- Practice conversation

sealed class ChatLine {
    data class Partner(val en: String, val gu: String) : ChatLine()
    data class You(val en: String, val gu: String, val clear: Boolean?) : ChatLine()
}

data class ChatState(
    val partnerName: String,
    val title: String,
    val lines: List<ChatLine>,
    /** Answer choices when it is the learner's turn. */
    val options: List<ConversationOption>,
    /** The option the learner picked and must now say. */
    val picked: ConversationOption?,
    val finished: Boolean
)

/** Step 2: a scripted conversation; the learner picks an answer and says it. */
class SpeakChatViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val unitId: String = handle["unitId"] ?: ""
    private val unit: SpeakingUnit? = repository.speakingUnit(unitId)
    private val lines = mutableListOf<ChatLine>()
    private var turn = 0
    private var picked: ConversationOption? = null
    var checkAvailable = true

    private val _state = MutableLiveData<ChatState>()
    val state: LiveData<ChatState> = _state

    /** Text the screen should read aloud next (partner lines). */
    private val _speak = MutableLiveData<String?>()
    val speak: LiveData<String?> = _speak

    init { advance() }

    /** Shows partner lines until it is the learner's turn or the script ends. */
    private fun advance() {
        val script = unit?.conversation.orEmpty()
        while (turn < script.size && script[turn].who != "you") {
            val t = script[turn]
            lines += ChatLine.Partner(t.en, t.gu)
            _speak.value = t.en
            turn++
        }
        if (turn >= script.size) {
            viewModelScope.launch { repository.saveConversationDone(unitId, checkAvailable) }
        }
        publish()
    }

    fun pick(option: ConversationOption) {
        picked = option
        publish()
    }

    /** Called with what speech recognition heard, or null when the learner just says it without a check. */
    fun answered(heard: String?) {
        val option = picked ?: return
        val clear = heard?.let { SpeechMatcher.compare(option.en, it).clear }
        lines += ChatLine.You(option.en, option.gu, clear)
        picked = null
        turn++
        advance()
    }

    fun spoken() { _speak.value = null }

    private fun publish() {
        val script = unit?.conversation.orEmpty()
        val current = script.getOrNull(turn)
        _state.value = ChatState(
            partnerName = repository.partnerName,
            title = unit?.title.orEmpty(),
            lines = lines.toList(),
            options = if (current != null && current.who == "you" && picked == null) current.options else emptyList(),
            picked = picked,
            finished = turn >= script.size
        )
    }
}
