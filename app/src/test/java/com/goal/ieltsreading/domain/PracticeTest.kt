package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.SentenceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeTest {

    private val sentence = SentenceEntity(
        id = "S1-001", level = 1, pattern = "past simple",
        en = "Yesterday I went to the market.", gu = "ગઈકાલે હું બજારે ગયો.",
        alternatives = listOf("I went to the market yesterday."),
        tip = "", tipGu = "", focusWord = "went", focusWrong = "go", focusOptions = listOf("go", "went", "goes")
    )

    @Test
    fun sentenceCheckIgnoresCaseAndPunctuation() {
        assertTrue(SentenceChecker.isCorrect(sentence, "yesterday i went to the market"))
        assertTrue(SentenceChecker.isCorrect(sentence, "I went to the market yesterday."))
        assertFalse(SentenceChecker.isCorrect(sentence, "Yesterday I go to the market."))
    }

    @Test
    fun focusWordIsFoundIgnoringPunctuation() {
        assertEquals(2, SentenceChecker.focusIndex(sentence))
        assertEquals(6, SentenceChecker.tiles(sentence.en).size)
    }

    @Test
    fun speechMatchCountsHeardWords() {
        val clear = SpeechMatcher.compare("I usually get up at six.", "i usually get up at 6")
        assertTrue(clear.clear)
        assertEquals(6, clear.matched)
        val unclear = SpeechMatcher.compare("I usually get up at six.", "I get sick")
        assertFalse(unclear.clear)
    }
}
