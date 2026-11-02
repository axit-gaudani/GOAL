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
