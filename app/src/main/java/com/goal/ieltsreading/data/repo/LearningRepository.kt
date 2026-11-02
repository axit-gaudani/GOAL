package com.goal.ieltsreading.data.repo

import android.content.Context
import com.goal.ieltsreading.data.content.AbcFileJson
import com.goal.ieltsreading.data.content.AbcLetter
import com.goal.ieltsreading.data.content.ContentSeeder
import com.goal.ieltsreading.data.content.SpeakingFileJson
import com.goal.ieltsreading.data.db.SentenceEntity
import com.goal.ieltsreading.data.db.SentenceProgressEntity
import com.goal.ieltsreading.data.db.SpeakingProgressEntity
import com.goal.ieltsreading.data.db.VocabProgressEntity
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
    private val context: Context,
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
        // Learners who already have progress in Level 0+ (from before ABC existed) skip ABC automatically.
        val skipAbc = prefs.skipAbc || progressMap.keys.any { !it.startsWith("L-1-") }
        var previousCompleted = true
        return lessons.map { lesson ->
            val p = progressMap[lesson.id]
            val isAbc = lesson.levelId == ContentSeeder.ABC_LEVEL_ID
            val item = LessonItem(lesson, p, unlocked = unlockAll || previousCompleted || (isAbc && skipAbc))
            // Learners who know the letters may skip the ABC level: it then does not block Level 0.
            previousCompleted = p?.completed == true || (isAbc && skipAbc && previousCompleted)
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
        val packIds = packs().filter { it.unlocked }.map { it.pack.id }
        val owners = lessonIds + testIds + packIds
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

    suspend fun highestUnlockedLevelOrder(): Int =
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

    // ---------------------------------------------------------------- Sentence practice

    /** Level used for sentences and word packs: the highest level the learner has opened. */
    private suspend fun practiceLevel(): Int =
        if (prefs.unlockAll) 6 else highestUnlockedLevelOrder().coerceAtLeast(0)

    /**
     * Builds a round of [size] sentences: up to 4 the learner got wrong or that are due again,
     * then new sentences (current level first). [only] limits the round to one exercise type.
     */
    suspend fun sentenceRound(only: SentenceExercise?, size: Int = 10): List<RoundItem> {
        ready()
        val level = practiceLevel()
        val today = Dates.today()
        val dueIds = progress.dueSentences(today, 4).map { it.sentenceId }
        val due = content.sentences(dueIds)
        val practised = progress.practisedSentenceIds().toSet()
        val all = content.sentencesUpTo(level)
        // Shuffle first, then a stable sort keeps the newest level first in random order.
        val fresh = all.filter { it.id !in practised }.shuffled().sortedByDescending { it.level }
        val chosen = (due + fresh).distinctBy { it.id }.take(size).toMutableList()
        if (chosen.size < size) {
            chosen += all.filter { s -> chosen.none { it.id == s.id } }.shuffled().take(size - chosen.size)
        }
        val dueSet = dueIds.toSet()
        var last: SentenceExercise? = null
        return chosen.shuffled().map { s ->
            val allowed = SentenceExercise.entries.filter { it.minLevel <= maxOf(s.level, level.coerceAtMost(2)) }
            val type = if (only != null && only.minLevel <= level) only
            else allowed.filter { it != last }.randomOrNull() ?: SentenceExercise.BUILD
            last = type
            RoundItem(s, type, s.id in dueSet)
        }
    }

    /** Saves one answer: right answers wait longer before coming back, wrong ones return tomorrow. */
    suspend fun recordSentence(sentenceId: String, correct: Boolean) {
        val today = Dates.today()
        val old = progress.sentenceProgress(sentenceId)
        val box = if (correct) ((old?.box ?: 0) + 1).coerceAtMost(5) else 1
        val interval = if (correct) listOf(0, 1, 3, 7, 15, 30)[box] else 1
        progress.upsertSentenceProgress(
            SentenceProgressEntity(
                sentenceId = sentenceId,
                box = box,
                nextReviewDay = today + interval,
                timesCorrect = (old?.timesCorrect ?: 0) + if (correct) 1 else 0,
                timesWrong = (old?.timesWrong ?: 0) + if (correct) 0 else 1,
                lastCorrect = correct
            )
        )
        updateToday { it.copy(questionsAnswered = it.questionsAnswered + 1) }
    }

    suspend fun dueSentenceCount(): Int { ready(); return progress.dueSentenceCount(Dates.today()) }

    /** Grammar patterns, weakest first. */
    suspend fun patternMastery(): List<PatternMastery> =
        progress.patternStats().map {
            val attempts = it.correct + it.wrong
            PatternMastery(it.pattern, if (attempts == 0) 0 else it.correct * 100 / attempts, attempts)
        }.sortedBy { it.percent }

    suspend fun currentLevelName(): String {
        val order = practiceLevel()
        val level = content.levels().firstOrNull { it.sortOrder == order }
        return "Level $order" + (level?.let { " · ${it.title}" } ?: "")
    }

    // ---------------------------------------------------------------- Vocabulary builder

    suspend fun packs(): List<PackItem> {
        ready()
        val level = practiceLevel()
        val deck = progress.deckWordIds().toSet()
        return content.packs().map { p ->
            val words = content.packWords(p.id)
            PackItem(p, words.size, words.count { it.id in deck }, prefs.unlockAll || p.minLevel <= level)
        }
    }

    /** The pack the learner is working on: the chosen one, else the first open pack with words left. */
    suspend fun currentPack(): PackItem? {
        val packs = packs()
        return packs.firstOrNull { it.pack.id == prefs.currentPackId && it.unlocked }
            ?: packs.firstOrNull { it.unlocked && it.learned < it.total }
            ?: packs.firstOrNull { it.unlocked }
    }

    /** The next [count] words of a pack that are not in the learner's deck yet. */
    suspend fun newWords(packId: String, count: Int = 5): List<VocabEntity> {
        val deck = progress.deckWordIds().toSet()
        return content.packWords(packId).filter { it.id !in deck }.take(count)
    }

    suspend fun word(id: String): VocabEntity? = content.word(id)

    /** Adds a word to the review deck. A word the learner already knows starts in a higher box. */
    suspend fun learnWord(wordId: String, alreadyKnown: Boolean) {
        val today = Dates.today()
        val item = if (alreadyKnown) VocabProgressEntity(wordId, box = 3, nextReviewDay = today + 4, timesCorrect = 1, timesWrong = 0)
        else SpacedRepetition.newWord(wordId, today + 1)
        progress.insertVocabProgressIfMissing(listOf(item))
        updateToday { it.copy(wordsReviewed = it.wordsReviewed + 1) }
    }

    /** Words for a game: from the deck, topped up with words of the current pack. */
    suspend fun gameWords(count: Int): List<VocabEntity> {
        ready()
        val fromDeck = progress.randomDeckWords(count).mapNotNull { content.word(it.id) }
        if (fromDeck.size >= count) return fromDeck
        val packId = currentPack()?.pack?.id ?: return fromDeck
        return (fromDeck + content.packWords(packId).shuffled()).distinctBy { it.id }.take(count)
    }

    // ---------------------------------------------------------------- Level −1 ABC

    private var abcCache: AbcFileJson? = null

    /** Letters taught in an ABC lesson (empty for every other lesson). Small file, read once. */
    fun abcLetters(lessonId: String): List<AbcLetter> {
        val file = abcCache ?: try {
            context.assets.open("content/abc.json").bufferedReader(Charsets.UTF_8).use {
                gson.fromJson(it.readText(), AbcFileJson::class.java)
            }
        } catch (e: Exception) {
            AbcFileJson()
        }.also { abcCache = it }
        return file.lessons[lessonId].orEmpty()
    }

    // ---------------------------------------------------------------- Basic speaking

    private var speakingCache: SpeakingFileJson? = null

    private fun speakingFile(): SpeakingFileJson {
        speakingCache?.let { return it }
        val file = try {
            context.assets.open("content/speaking.json").bufferedReader(Charsets.UTF_8).use {
                gson.fromJson(it.readText(), SpeakingFileJson::class.java)
            }
        } catch (e: Exception) {
            SpeakingFileJson()
        }
        speakingCache = file
        return file
    }

    val partnerName: String get() = speakingFile().partnerName

    /** Units unlock one after another. */
    suspend fun speakingUnits(): List<SpeakingUnitItem> {
        val map = progress.speakingProgress().associateBy { it.unitId }
        var previousDone = true
        return speakingFile().units.sortedBy { it.order }.map { u ->
            val p = map[u.id]
            val item = SpeakingUnitItem(u, p, prefs.unlockAll || previousDone)
            previousDone = p?.completed == true
            item
        }
    }

    fun speakingUnit(unitId: String) = speakingFile().units.firstOrNull { it.id == unitId }

    /** A different phrase every day from the units the learner can open. */
    suspend fun phraseOfTheDay(): com.goal.ieltsreading.data.content.SpeakingPhrase? {
        val phrases = speakingUnits().filter { it.unlocked }.flatMap { it.unit.phrases }
        if (phrases.isEmpty()) return null
        return phrases[(Dates.today() % phrases.size).toInt()]
    }

    /**
     * Saves the phrase step. [clear] = phrases the speech check heard clearly; when the phone
     * has no speech check, practising every phrase counts instead.
     */
    suspend fun savePhraseStep(unitId: String, clear: Int, practised: Int, total: Int, checkAvailable: Boolean) {
        val old = progress.speakingProgress(unitId)
        val bestClear = maxOf(clear, old?.phrasesClear ?: 0)
        val bestPractised = maxOf(practised, old?.phrasesPracticed ?: 0)
        val phrasesPassed = if (checkAvailable) bestClear >= minOf(6, total) else bestPractised >= total
        val conversation = old?.conversationDone == true
        progress.upsertSpeakingProgress(
            SpeakingProgressEntity(unitId, bestClear, bestPractised, conversation, phrasesPassed && conversation)
        )
    }

    suspend fun saveConversationDone(unitId: String, checkAvailable: Boolean) {
        val old = progress.speakingProgress(unitId)
        val total = speakingUnit(unitId)?.phrases?.size ?: 8
        val phrasesPassed = if (checkAvailable) (old?.phrasesClear ?: 0) >= minOf(6, total)
        else (old?.phrasesPracticed ?: 0) >= total
        progress.upsertSpeakingProgress(
            SpeakingProgressEntity(unitId, old?.phrasesClear ?: 0, old?.phrasesPracticed ?: 0, true, phrasesPassed)
        )
        updateToday { it.copy(lessonsCompleted = it.lessonsCompleted + if (phrasesPassed && old?.completed != true) 1 else 0) }
    }
}
