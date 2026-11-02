package com.goal.ieltsreading.data.repo

import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.LessonProgressEntity
import com.goal.ieltsreading.data.db.LevelEntity
import com.goal.ieltsreading.data.db.TestAttemptEntity
import com.goal.ieltsreading.data.db.TestEntity

data class LevelItem(
    val level: LevelEntity,
    val totalLessons: Int,
    val completedLessons: Int,
    val unlocked: Boolean
) {
    val percent: Int get() = if (totalLessons == 0) 0 else completedLessons * 100 / totalLessons
    val finished: Boolean get() = totalLessons > 0 && completedLessons == totalLessons
}

data class LessonItem(
    val lesson: LessonEntity,
    val progress: LessonProgressEntity?,
    val unlocked: Boolean
) {
    val completed: Boolean get() = progress?.completed == true
}

data class TestItem(
    val test: TestEntity,
    val unlocked: Boolean,
    val requiredLevelTitle: String,
    val bestBand: Double?,
    val attempts: Int
)

data class DayStat(val day: Long, val minutes: Int, val reachedGoal: Boolean)

data class TodaySummary(
    val minutesToday: Int,
    val goalMinutes: Int,
    val lessonsToday: Int,
    val wordsToday: Int,
    val streak: Int
)

data class OverallStats(
    val lessonsCompleted: Int,
    val totalLessons: Int,
    val wordsInDeck: Int,
    val wordsMastered: Int,
    val bestBand: Double?,
    val averageLessonScore: Int,
    val totalMinutes: Int,
    val streak: Int,
    val week: List<DayStat>,
    val levels: List<LevelItem>,
    val recentAttempts: List<Pair<TestAttemptEntity, String>>
)

// ---------------- Sentence practice ----------------

enum class SentenceExercise(val title: String, val titleGu: String, val minLevel: Int) {
    BUILD("Build the sentence", "શબ્દો ગોઠવો", 0),
    FILL("Fill the gap", "ખાલી જગ્યા ભરો", 0),
    FIX("Fix the mistake", "ભૂલ સુધારો", 1),
    DICTATION("Listen and write", "સાંભળીને લખો", 1),
    TRANSLATE("Translate", "ગુજરાતી → અંગ્રેજી", 2)
}

data class RoundItem(val sentence: com.goal.ieltsreading.data.db.SentenceEntity, val exercise: SentenceExercise, val isRepeat: Boolean)

data class PatternMastery(val pattern: String, val percent: Int, val attempts: Int)

// ---------------- Vocabulary builder ----------------

data class PackItem(
    val pack: com.goal.ieltsreading.data.db.WordPackEntity,
    val total: Int,
    val learned: Int,
    val unlocked: Boolean
) {
    val percent: Int get() = if (total == 0) 0 else learned * 100 / total
}

// ---------------- Basic speaking ----------------

data class SpeakingUnitItem(
    val unit: com.goal.ieltsreading.data.content.SpeakingUnit,
    val progress: com.goal.ieltsreading.data.db.SpeakingProgressEntity?,
    val unlocked: Boolean
) {
    val completed: Boolean get() = progress?.completed == true
}
