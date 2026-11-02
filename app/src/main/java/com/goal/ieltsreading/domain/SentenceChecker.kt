package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.SentenceEntity

/** Checks typed or built sentences. Ignores case, extra spaces and final punctuation. */
object SentenceChecker {

    fun normalize(text: String): String = text.trim()
        .lowercase()
        .replace("’", "'")
        .replace(Regex("[.,!?;:\"]"), "")
        .replace(Regex("\\s+"), " ")

    fun isCorrect(sentence: SentenceEntity, answer: String): Boolean {
        val a = normalize(answer)
        if (a.isEmpty()) return false
        return (listOf(sentence.en) + sentence.alternatives).any { normalize(it) == a }
    }

    /** The sentence split into word tiles (punctuation stays on its word). */
    fun tiles(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    /** Index of the focus word inside the tiles, ignoring punctuation. */
    fun focusIndex(sentence: SentenceEntity): Int =
        tiles(sentence.en).indexOfFirst { it.trim('.', ',', '!', '?', ';', ':', '"') == sentence.focusWord }
}
