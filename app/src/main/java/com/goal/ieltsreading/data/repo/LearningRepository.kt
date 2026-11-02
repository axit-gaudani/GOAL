package com.goal.ieltsreading.data.repo

import com.goal.ieltsreading.data.content.ContentSeeder
import com.goal.ieltsreading.data.db.AppDatabase
import com.goal.ieltsreading.data.db.DailyActivityEntity
import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.LessonProgressEntity
import com.goal.ieltsreading.data.db.QuestionEntity
import com.goal.ieltsreading.data.db.TestAttemptEntity
import com.goal.ieltsreading.data.db.TestEntity
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.data.db.WordWithProgress
import com.goal.ieltsreading.data.prefs.UserPrefs
import com.goal.ieltsreading.domain.Dates
import com.goal.ieltsreading.domain.SpacedRepetition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single place where screens get data from. ViewModels call these suspend
 * functions; Room runs the queries on a background thread.
 */
class LearningRepository(
    private val db: AppDatabase,
    private val seeder: ContentSeeder,
    val prefs: UserPrefs
) {
    companion object {
        /** A lesson is "passed" (and the next one unlocks) at this practice score. */
        const val PASS_PERCENT = 60
        /** Seconds per screen visit we count at most, so a phone left open doesn't inflate study time. */
        const val MAX_SESSION_SECONDS = 30 * 60L
    }

    private val content = db.contentDao()
    private val progress = db.progressDao()
    private val gson = Gson()
    private val seedLock = Mutex()
    @Volatile private var seeded = false

    private suspend fun ready() {
        if (seeded) return
        seedLock.withLock {
            if (!seeded) {
                seeder.seedIfNeeded()
                seeded = true
            }
        }
    }

    // ---------------------------------------------------------------- Levels & lessons

    private suspend fun lessonItems(): List<LessonItem> {
        ready()
        val lessons = content.allLessons()
        val progressMap = progress.lessonProgress().associateBy { it.lessonId }
        val unlockAll = prefs.unlockAll
        var previousCompleted = true
        return lessons.map { lesson ->
            val p = progressMap[lesson.id]
            val item = LessonItem(lesson, p, unlocked = unlockAll || previousCompleted)
            previousCompleted = p?.completed == true
            item
        }
    }

    suspend fun levels(): List<LevelItem> {
        val lessons = lessonItems().groupBy { it.lesson.levelId }
        return content.levels().map { level ->
            val list = lessons[level.id].orEmpty()
            LevelItem(
                level = level,
                totalLessons = list.size,
                completedLessons = list.count { it.completed },
                unlocked = list.firstOrNull()?.unlocked ?: false
            )
        }
    }

    suspend fun level(levelId: String) = run { ready(); content.level(levelId) }

    suspend fun lessonsForLevel(levelId: String): List<LessonItem> =
        lessonItems().filter { it.lesson.levelId == levelId }

    /** The next lesson the student should do: the first unlocked lesson that is not completed. */
    suspend fun nextLesson(): LessonItem? {
        val items = lessonItems()
        return items.firstOrNull { it.unlocked && !it.completed }
    }

    suspend fun lessonAfter(lessonId: String): LessonItem? {
        val items = lessonItems()
        val index = items.indexOfFirst { it.lesson.id == lessonId }
        return items.getOrNull(index + 1)?.takeIf { it.unlocked }
    }

    suspend fun lesson(lessonId: String): LessonEntity? = run { ready(); content.lesson(lessonId) }

    suspend fun questions(ownerId: String): List<QuestionEntity> = run { ready(); content.questions(ownerId) }

    suspend fun vocabulary(ownerId: String): List<VocabEntity> = run { ready(); content.vocabulary(ownerId) }

    suspend fun lessonProgress(lessonId: String) = progress.lessonProgress(lessonId)

    /** Saves a practice result. Returns true when the lesson is passed. */
    suspend fun saveLessonResult(lessonId: String, correct: Int, total: Int): Boolean {
        ready()
        val percent = if (total == 0) 100 else correct * 100 / total
        val passed = percent >= PASS_PERCENT
        val old = progress.lessonProgress(lessonId)
        val firstCompletion = passed && old?.completed != true
        progress.upsertLessonProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                completed = passed || old?.completed == true,
                bestScore = maxOf(percent, old?.bestScore ?: 0),
                attempts = (old?.attempts ?: 0) + 1,
                lastAttemptAt = System.currentTimeMillis()
            )
        )
        if (passed) addLessonWordsToDeck(lessonId)
        updateToday { it.copy(
            lessonsCompleted = it.lessonsCompleted + if (firstCompletion) 1 else 0,
            questionsAnswered = it.questionsAnswered + total
        ) }
        return passed
    }

    // ---------------------------------------------------------------- Vocabulary

    suspend fun addLessonWordsToDeck(ownerId: String) {
        val today = Dates.today()
        val words = content.vocabulary(ownerId)
        progress.insertVocabProgressIfMissing(words.map { SpacedRepetition.newWord(it.id, today) })
    }

    /** Words from every lesson the student can open, plus test glossaries, with their review state. */
    suspend fun availableWords(): List<WordWithProgress> {
        val lessonIds = lessonItems().filter { it.unlocked }.map { it.lesson.id }
        val testIds = tests().filter { it.unlocked }.map { it.test.id }
        val owners = lessonIds + testIds
        if (owners.isEmpty()) return emptyList()
        return progress.wordsFor(owners)
    }

    suspend fun dueWords(limit: Int = 20): List<WordWithProgress> { ready(); return progress.dueWords(Dates.today(), limit) }
    suspend fun dueCount(): Int { ready(); return progress.dueCount(Dates.today()) }
    suspend fun deckSize(): Int = progress.deckSize()
    suspend fun masteredCount(): Int = progress.masteredCount()

    suspend fun addWordToDeck(wordId: String) {
        progress.insertVocabProgressIfMissing(listOf(SpacedRepetition.newWord(wordId, Dates.today())))
    }

    suspend fun reviewWord(wordId: String, knewIt: Boolean) {
        val today = Dates.today()
        val current = progress.vocabProgress(wordId) ?: SpacedRepetition.newWord(wordId, today)
        progress.upsertVocabProgress(SpacedRepetition.review(current, knewIt, today))
        updateToday { it.copy(wordsReviewed = it.wordsReviewed + 1) }
    }

    /** A different word every day, chosen from lessons the student has opened. */
    suspend fun wordOfTheDay(): WordWithProgress? {
        val words = availableWords().filter { it.levelId != ContentSeeder.TEST_LEVEL_ID }
        if (words.isEmpty()) return null
        return words[(Dates.today() % words.size).toInt()]
    }

    // ---------------------------------------------------------------- Practice tests

    private suspend fun highestUnlockedLevelOrder(): Int =
        levels().filter { it.unlocked }.maxOfOrNull { it.level.sortOrder } ?: 0

    suspend fun tests(): List<TestItem> {
        ready()
        val maxLevel = highestUnlockedLevelOrder()
        val levelTitles = content.levels().associate { it.sortOrder to it.title }
        val attempts = progress.attempts().groupBy { it.testId }
        return content.tests().map { t ->
            val list = attempts[t.id].orEmpty()
            TestItem(
                test = t,
                unlocked = prefs.unlockAll || t.requiredLevel <= maxLevel,
                requiredLevelTitle = "Level ${t.requiredLevel} · ${levelTitles[t.requiredLevel].orEmpty()}",
                bestBand = list.maxOfOrNull { it.band },
                attempts = list.size
            )
        }
    }

    suspend fun test(testId: String): TestEntity? = run { ready(); content.test(testId) }

    suspend fun saveTestAttempt(
        testId: String, correct: Int, total: Int, band: Double, durationSeconds: Int, answers: Map<String, String>
    ): Long {
        updateToday { it.copy(questionsAnswered = it.questionsAnswered + total) }
        return progress.insertAttempt(
            TestAttemptEntity(
                testId = testId, correct = correct, total = total, band = band,
                durationSeconds = durationSeconds, takenAt = System.currentTimeMillis(),
                answersJson = gson.toJson(answers)
            )
        )
    }

    suspend fun attempt(id: Long): TestAttemptEntity? = progress.attempt(id)

    fun answersOf(attempt: TestAttemptEntity): Map<String, String> =
        gson.fromJson(attempt.answersJson, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()

    // ---------------------------------------------------------------- Daily activity & stats

    private suspend fun updateToday(change: (DailyActivityEntity) -> DailyActivityEntity) {
        val today = Dates.today()
        val current = progress.activity(today) ?: DailyActivityEntity(day = today)
        progress.upsertActivity(change(current))
    }

    suspend fun addStudySeconds(seconds: Long) {
        if (seconds <= 0) return
        val counted = seconds.coerceAtMost(MAX_SESSION_SECONDS)
        updateToday { it.copy(studySeconds = it.studySeconds + counted) }
    }

    /** Number of days in a row (ending today or yesterday) with some learning activity. */
    suspend fun streak(): Int {
        val days = progress.activeDaysDesc()
        if (days.isEmpty()) return 0
        val today = Dates.today()
        var expected = if (days.first() == today) today else today - 1
        var count = 0
        for (d in days) {
            if (d == expected) { count++; expected-- } else if (d < expected) break
        }
        return count
    }

    suspend fun todaySummary(): TodaySummary {
        ready()
        val a = progress.activity(Dates.today())
        return TodaySummary(
            minutesToday = ((a?.studySeconds ?: 0) / 60).toInt(),
            goalMinutes = prefs.dailyGoalMinutes,
            lessonsToday = a?.lessonsCompleted ?: 0,
            wordsToday = a?.wordsReviewed ?: 0,
            streak = streak()
        )
    }

    suspend fun overallStats(): OverallStats {
        val lessons = lessonItems()
        val levels = levels()
        val today = Dates.today()
        val activity = progress.activitySince(today - 6).associateBy { it.day }
        val goal = prefs.dailyGoalMinutes
        val week = (6 downTo 0).map { back ->
            val day = today - back
            val minutes = ((activity[day]?.studySeconds ?: 0) / 60).toInt()
            DayStat(day, minutes, minutes >= goal)
        }
        val attempts = progress.attempts()
        val testTitles = content.tests().associate { it.id to it.title }
        val completed = lessons.filter { it.completed }
        val totalMinutes = (progress.activitySince(0).sumOf { it.studySeconds } / 60).toInt()
        return OverallStats(
            lessonsCompleted = completed.size,
            totalLessons = lessons.size,
            wordsInDeck = progress.deckSize(),
            wordsMastered = progress.masteredCount(),
            bestBand = attempts.maxOfOrNull { it.band },
            averageLessonScore = if (completed.isEmpty()) 0 else completed.sumOf { it.progress?.bestScore ?: 0 } / completed.size,
            totalMinutes = totalMinutes,
            streak = streak(),
            week = week,
            levels = levels,
            recentAttempts = attempts.take(10).map { it to (testTitles[it.testId] ?: it.testId) }
        )
    }

    suspend fun resetProgress() = progress.resetAll()
}
