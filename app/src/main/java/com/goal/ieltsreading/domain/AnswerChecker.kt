package com.goal.ieltsreading.domain

import com.goal.ieltsreading.data.db.QuestionEntity

/** Decides whether a student's answer is correct. */
object AnswerChecker {

    fun isCorrect(question: QuestionEntity, userAnswer: String?): Boolean {
        if (userAnswer.isNullOrBlank()) return false
        val user = normalize(userAnswer)
        val accepted = (listOf(question.answer) + question.alternatives).map { normalize(it) }
        if (user in accepted) return true
        // For typed answers, ignore a leading article ("the river" == "river").
        if (question.type == QuestionType.FILL) {
            val noArticle = stripArticle(user)
            return accepted.any { stripArticle(it) == noArticle }
        }
        return false
    }

    fun normalize(text: String): String =
        text.trim()
            .lowercase()
            .replace(Regex("[\\u2018\\u2019`]"), "'")
            .replace(Regex("[.,!?;:\"]+$"), "")
            .replace(Regex("\\s+"), " ")

    private fun stripArticle(text: String): String = text.removePrefix("the ").removePrefix("a ").removePrefix("an ")
}
