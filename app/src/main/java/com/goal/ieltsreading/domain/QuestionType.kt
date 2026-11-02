package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.QuestionEntity

object QuestionType {
    const val MCQ = "MCQ"
    const val TFNG = "TFNG"
    const val YNNG = "YNNG"
    const val FILL = "FILL"
    /** The app reads the prompt aloud (it stays hidden); the learner picks what they heard. */
    const val LISTEN = "LISTEN"

    val TFNG_OPTIONS = listOf("TRUE", "FALSE", "NOT GIVEN")
    val YNNG_OPTIONS = listOf("YES", "NO", "NOT GIVEN")

    /** The choices to show as buttons. FILL questions have none (the student types the answer). */
    fun optionsFor(q: QuestionEntity): List<String> = when (q.type) {
        TFNG -> TFNG_OPTIONS
        YNNG -> YNNG_OPTIONS
        FILL -> emptyList()
        else -> q.options
    }

    fun label(type: String): String = when (type) {
        TFNG -> "True / False / Not Given"
        YNNG -> "Yes / No / Not Given"
        FILL -> "Complete the sentence"
        LISTEN -> "Listen and choose"
        else -> "Choose the correct answer"
    }

    fun labelGu(type: String): String = when (type) {
        TFNG -> "વિધાન લખાણ સાથે મેળ ખાય છે? (TRUE / FALSE / NOT GIVEN)"
        YNNG -> "લેખકના મત સાથે મેળ ખાય છે? (YES / NO / NOT GIVEN)"
        FILL -> "ખાલી જગ્યા લખાણમાંથી શબ્દ લઈને ભરો"
        LISTEN -> "સાંભળો અને સાચો જવાબ પસંદ કરો"
        else -> "સાચો જવાબ પસંદ કરો"
    }
}
