package com.goal.ieltsreading.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------------- Learning content (re-created from assets when content changes) ----------------

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val title: String,
    val titleGu: String,
    val band: String,
    val description: String,
    val descriptionGu: String
)

data class Example(val en: String, val gu: String)

@Entity(tableName = "lessons", indices = [Index("levelId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    val levelId: String,
    /** Order inside the level. */
    val sortOrder: Int,
    /** Order across the whole course; used to unlock lessons one by one. */
    val globalOrder: Int,
    val title: String,
    val titleGu: String,
    val skill: String,
    val minutes: Int,
    val explanation: String,
    val explanationGu: String,
    val examples: List<Example>,
    val passageTitle: String,
    val passage: String,
    val passageGu: String
)

@Entity(tableName = "vocabulary", indices = [Index("ownerId")])
data class VocabEntity(
    /** Stable id "<ownerId>:<word>" so review progress survives content updates. */
    @PrimaryKey val id: String,
    /** Lesson id or test id. */
    val ownerId: String,
    val levelId: String,
    val word: String,
    val pos: String,
    val meaningGu: String,
    val meaningEn: String,
    val example: String,
    val exampleGu: String
)

@Entity(tableName = "questions", indices = [Index("ownerId")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    /** Lesson id or test id. */
    val ownerId: String,
    val sortOrder: Int,
    val type: String,
    val prompt: String,
    val promptGu: String,
    val options: List<String>,
    val answer: String,
    val alternatives: List<String>,
    val explanation: String,
    val explanationGu: String,
    val wordLimit: String
)

@Entity(tableName = "practice_tests")
data class TestEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val title: String,
    val titleGu: String,
    val targetBand: String,
    val requiredLevel: Int,
    val minutes: Int,
    val passageTitle: String,
    val passage: String
)

// ---------------- User progress (never deleted by content updates) ----------------

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val completed: Boolean,
    val bestScore: Int,
    val attempts: Int,
    val lastAttemptAt: Long
)

@Entity(tableName = "vocab_progress")
data class VocabProgressEntity(
    @PrimaryKey val wordId: String,
    /** Leitner box 1..5. Box 5 = mastered. */
    val box: Int,
    /** Epoch day when this word should be reviewed again. */
    val nextReviewDay: Long,
    val timesCorrect: Int,
    val timesWrong: Int
)

@Entity(tableName = "test_attempts", indices = [Index("testId")])
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: String,
    val correct: Int,
    val total: Int,
    val band: Double,
    val durationSeconds: Int,
    val takenAt: Long,
    /** JSON map questionId -> user answer, used for the review screen. */
    val answersJson: String
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val day: Long,
    val studySeconds: Long = 0,
    val lessonsCompleted: Int = 0,
    val wordsReviewed: Int = 0,
    val questionsAnswered: Int = 0
)

/** Vocabulary joined with its review progress (progress columns are null when the word is not in the deck yet). */
data class WordWithProgress(
    val id: String,
    val ownerId: String,
    val levelId: String,
    val word: String,
    val pos: String,
    val meaningGu: String,
    val meaningEn: String,
    val example: String,
    val exampleGu: String,
    val box: Int?,
    val nextReviewDay: Long?
)
