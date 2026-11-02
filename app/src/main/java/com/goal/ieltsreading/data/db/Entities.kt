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
    val exampleGu: String,
    /** How the word sounds, written in Gujarati letters (e.g. market → માર્કેટ). */
    val pronunciationGu: String = "",
    /** Other forms of the word: protect, protection, protective. */
    val family: List<WordForm> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val opposites: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val ieltsTip: String = "",
    /** Order inside a word pack. */
    val sortOrder: Int = 0
)

data class WordForm(val word: String, val pos: String, val gu: String)

/** A topic pack in the Vocabulary builder (Daily life, Environment...). Its words are in [VocabEntity] with ownerId = pack id. */
@Entity(tableName = "word_packs")
data class WordPackEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val title: String,
    val titleGu: String,
    val ielts: Boolean,
    /** Level order the learner must reach before the pack opens. */
    val minLevel: Int
)

/** One practice sentence for Sentence practice. */
@Entity(tableName = "sentences", indices = [Index("level")])
data class SentenceEntity(
    @PrimaryKey val id: String,
    val level: Int,
    /** Grammar pattern, e.g. "past simple"; used for mastery per pattern. */
    val pattern: String,
    val en: String,
    val gu: String,
    val alternatives: List<String>,
    val tip: String,
    val tipGu: String,
    /** The grammar word the sentence practises (e.g. "went"). */
    val focusWord: String,
    /** A typical mistake for that word (e.g. "go"). */
    val focusWrong: String,
    val focusOptions: List<String>
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

@Entity(tableName = "sentence_progress")
data class SentenceProgressEntity(
    @PrimaryKey val sentenceId: String,
    val box: Int,
    val nextReviewDay: Long,
    val timesCorrect: Int,
    val timesWrong: Int,
    val lastCorrect: Boolean
)

@Entity(tableName = "speaking_progress")
data class SpeakingProgressEntity(
    @PrimaryKey val unitId: String,
    /** Phrases the speech check heard clearly (best attempt). */
    val phrasesClear: Int,
    /** Phrases the learner practised at least once. */
    val phrasesPracticed: Int,
    val conversationDone: Boolean,
    val completed: Boolean
)

/** Correct / wrong answers per grammar pattern, for the mastery bars. */
data class PatternStat(val pattern: String, val correct: Int, val wrong: Int)

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
