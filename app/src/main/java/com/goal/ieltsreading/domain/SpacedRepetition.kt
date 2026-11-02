package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.VocabProgressEntity

/**
 * Simple Leitner system: every word sits in a box from 1 to 5.
 * Correct answer -> next box (longer wait). Wrong answer -> back to box 1.
 */
object SpacedRepetition {
    const val MAX_BOX = 5
    private val intervalDays = mapOf(1 to 1, 2 to 2, 3 to 4, 4 to 7, 5 to 15)

    fun newWord(wordId: String, today: Long) =
        VocabProgressEntity(wordId = wordId, box = 1, nextReviewDay = today, timesCorrect = 0, timesWrong = 0)

    fun review(current: VocabProgressEntity, knewIt: Boolean, today: Long): VocabProgressEntity =
        if (knewIt) {
            val box = (current.box + 1).coerceAtMost(MAX_BOX)
            current.copy(box = box, nextReviewDay = today + intervalDays.getValue(box), timesCorrect = current.timesCorrect + 1)
        } else {
            current.copy(box = 1, nextReviewDay = today + 1, timesWrong = current.timesWrong + 1)
        }
}
