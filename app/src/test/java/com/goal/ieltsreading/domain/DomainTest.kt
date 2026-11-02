package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.QuestionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {

    private fun question(type: String, answer: String, alternatives: List<String> = emptyList()) = QuestionEntity(
        id = "q", ownerId = "L0-01", sortOrder = 0, type = type, prompt = "", promptGu = "",
        options = emptyList(), answer = answer, alternatives = alternatives,
        explanation = "", explanationGu = "", wordLimit = ""
    )

    @Test
    fun fillAnswersIgnoreCaseSpacesAndArticles() {
        val q = question(QuestionType.FILL, "river", listOf("rivers"))
        assertTrue(AnswerChecker.isCorrect(q, "  River "))
        assertTrue(AnswerChecker.isCorrect(q, "the river"))
        assertTrue(AnswerChecker.isCorrect(q, "rivers."))
        assertFalse(AnswerChecker.isCorrect(q, "sea"))
        assertFalse(AnswerChecker.isCorrect(q, ""))
    }

    @Test
    fun tfngNeedsExactLabel() {
        val q = question(QuestionType.TFNG, "NOT GIVEN")
        assertTrue(AnswerChecker.isCorrect(q, "NOT GIVEN"))
        assertFalse(AnswerChecker.isCorrect(q, "FALSE"))
    }

    @Test
    fun bandConversionMatchesIeltsTable() {
        assertEquals(9.0, BandCalculator.bandFor(40, 40), 0.0)
        assertEquals(7.0, BandCalculator.bandFor(30, 40), 0.0)
        assertEquals(6.0, BandCalculator.bandFor(23, 40), 0.0)
        assertEquals(5.0, BandCalculator.bandFor(15, 40), 0.0)
        // 10 of 13 = 30.8 of 40 -> 31 -> Band 7
        assertEquals(7.0, BandCalculator.bandFor(10, 13), 0.0)
        assertEquals("6.5", BandCalculator.format(6.5))
        assertEquals("7", BandCalculator.format(7.0))
    }

    @Test
    fun leitnerBoxesMoveUpAndReset() {
        val today = 100L
        var p = SpacedRepetition.newWord("w", today)
        p = SpacedRepetition.review(p, knewIt = true, today = today)
        assertEquals(2, p.box)
        assertEquals(today + 2, p.nextReviewDay)
        p = SpacedRepetition.review(p, knewIt = false, today = today)
        assertEquals(1, p.box)
        assertEquals(today + 1, p.nextReviewDay)
        repeat(10) { p = SpacedRepetition.review(p, true, today) }
        assertEquals(SpacedRepetition.MAX_BOX, p.box)
    }
}
