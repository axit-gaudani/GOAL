package com.goal.ieltsreading.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface ContentDao {
    @Query("SELECT * FROM levels ORDER BY sortOrder")
    suspend fun levels(): List<LevelEntity>

    @Query("SELECT * FROM levels WHERE id = :id")
    suspend fun level(id: String): LevelEntity?

    @Query("SELECT * FROM lessons ORDER BY globalOrder")
    suspend fun allLessons(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT * FROM questions WHERE ownerId = :ownerId ORDER BY sortOrder")
    suspend fun questions(ownerId: String): List<QuestionEntity>

    @Query("SELECT * FROM vocabulary WHERE ownerId = :ownerId ORDER BY word COLLATE NOCASE")
    suspend fun vocabulary(ownerId: String): List<VocabEntity>

    @Query("SELECT * FROM practice_tests ORDER BY sortOrder")
    suspend fun tests(): List<TestEntity>

    @Query("SELECT * FROM practice_tests WHERE id = :id")
    suspend fun test(id: String): TestEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun lessonCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLevels(items: List<LevelEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLessons(items: List<LessonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVocab(items: List<VocabEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuestions(items: List<QuestionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTests(items: List<TestEntity>)

    @Query("DELETE FROM levels") suspend fun clearLevels()
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM vocabulary") suspend fun clearVocab()
    @Query("DELETE FROM questions") suspend fun clearQuestions()
    @Query("DELETE FROM practice_tests") suspend fun clearTests()

    /** Replaces all learning content in one transaction (user progress tables are untouched). */
    @Transaction
    suspend fun replaceAll(
        levels: List<LevelEntity>,
        lessons: List<LessonEntity>,
        vocab: List<VocabEntity>,
        questions: List<QuestionEntity>,
        tests: List<TestEntity>
    ) {
        clearLevels(); clearLessons(); clearVocab(); clearQuestions(); clearTests()
        insertLevels(levels); insertLessons(lessons); insertVocab(vocab)
        insertQuestions(questions); insertTests(tests)
    }
}

@Dao
interface ProgressDao {
    // Lessons
    @Query("SELECT * FROM lesson_progress")
    suspend fun lessonProgress(): List<LessonProgressEntity>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun lessonProgress(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsertLessonProgress(item: LessonProgressEntity)

    // Vocabulary review (spaced repetition)
    @Query(
        """SELECT v.id, v.ownerId, v.levelId, v.word, v.pos, v.meaningGu, v.meaningEn, v.example, v.exampleGu,
                  p.box AS box, p.nextReviewDay AS nextReviewDay
           FROM vocabulary v LEFT JOIN vocab_progress p ON p.wordId = v.id
           WHERE v.ownerId IN (:ownerIds)
           ORDER BY v.word COLLATE NOCASE"""
    )
    suspend fun wordsFor(ownerIds: List<String>): List<WordWithProgress>

    @Query(
        """SELECT v.id, v.ownerId, v.levelId, v.word, v.pos, v.meaningGu, v.meaningEn, v.example, v.exampleGu,
                  p.box AS box, p.nextReviewDay AS nextReviewDay
           FROM vocabulary v INNER JOIN vocab_progress p ON p.wordId = v.id
           WHERE p.nextReviewDay <= :today
           ORDER BY p.box, p.nextReviewDay
           LIMIT :limit"""
    )
    suspend fun dueWords(today: Long, limit: Int): List<WordWithProgress>

    @Query("SELECT COUNT(*) FROM vocab_progress p INNER JOIN vocabulary v ON v.id = p.wordId WHERE p.nextReviewDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT COUNT(*) FROM vocab_progress p INNER JOIN vocabulary v ON v.id = p.wordId")
    suspend fun deckSize(): Int

    @Query("SELECT COUNT(*) FROM vocab_progress p INNER JOIN vocabulary v ON v.id = p.wordId WHERE p.box >= 5")
    suspend fun masteredCount(): Int

    @Query("SELECT * FROM vocab_progress WHERE wordId = :wordId")
    suspend fun vocabProgress(wordId: String): VocabProgressEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVocabProgressIfMissing(items: List<VocabProgressEntity>)

    @Upsert
    suspend fun upsertVocabProgress(item: VocabProgressEntity)

    // Tests
    @Insert
    suspend fun insertAttempt(item: TestAttemptEntity): Long

    @Query("SELECT * FROM test_attempts WHERE id = :id")
    suspend fun attempt(id: Long): TestAttemptEntity?

    @Query("SELECT * FROM test_attempts ORDER BY takenAt DESC")
    suspend fun attempts(): List<TestAttemptEntity>

    // Daily activity
    @Query("SELECT * FROM daily_activity WHERE day = :day")
    suspend fun activity(day: Long): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity WHERE day >= :fromDay ORDER BY day")
    suspend fun activitySince(fromDay: Long): List<DailyActivityEntity>

    @Query("SELECT day FROM daily_activity WHERE studySeconds >= 60 OR lessonsCompleted > 0 OR wordsReviewed > 0 ORDER BY day DESC")
    suspend fun activeDaysDesc(): List<Long>

    @Upsert
    suspend fun upsertActivity(item: DailyActivityEntity)

    // Reset
    @Query("DELETE FROM lesson_progress") suspend fun clearLessonProgress()
    @Query("DELETE FROM vocab_progress") suspend fun clearVocabProgress()
    @Query("DELETE FROM test_attempts") suspend fun clearAttempts()
    @Query("DELETE FROM daily_activity") suspend fun clearActivity()

    @Transaction
    suspend fun resetAll() {
        clearLessonProgress(); clearVocabProgress(); clearAttempts(); clearActivity()
    }
}
