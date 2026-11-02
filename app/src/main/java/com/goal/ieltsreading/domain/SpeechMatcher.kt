package com.goal.ieltsreading.domain

/** Compares what speech recognition heard with the phrase the learner should say. */
object SpeechMatcher {

    data class Result(
        /** Each expected word and whether it was heard. */
        val words: List<Pair<String, Boolean>>,
        val matched: Int,
        val total: Int
    ) {
        /** "Clear" when at least 80% of the words were heard. */
        val clear: Boolean get() = total > 0 && matched * 100 / total >= 80
    }

    fun compare(expected: String, heard: String): Result {
        val heardWords = tokens(heard).toMutableList()
        val words = expected.split(Regex("\\s+")).filter { it.isNotBlank() }.map { raw ->
            val token = clean(raw)
            val found = heardWords.indexOf(token)
            if (found >= 0) heardWords.removeAt(found)
            raw to (found >= 0 || token.isEmpty())
        }
        return Result(words, words.count { it.second }, words.size)
    }

    private fun tokens(text: String) = text.split(Regex("\\s+")).map { clean(it) }.filter { it.isNotEmpty() }

    private fun clean(word: String) = word.lowercase()
        .replace("’", "'")
        .replace(Regex("[^a-z0-9']"), "")
        .let { NUMBERS[it] ?: it }

    /** Recognisers often return digits; treat them like the spelled words. */
    private val NUMBERS = mapOf(
        "1" to "one", "2" to "two", "3" to "three", "4" to "four", "5" to "five",
        "6" to "six", "7" to "seven", "8" to "eight", "9" to "nine", "10" to "ten"
    )
}
