package com.goal.ieltsreading.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LevelEntity::class, LessonEntity::class, VocabEntity::class, QuestionEntity::class, TestEntity::class,
        WordPackEntity::class, SentenceEntity::class,
        LessonProgressEntity::class, VocabProgressEntity::class, TestAttemptEntity::class, DailyActivityEntity::class,
        SentenceProgressEntity::class, SpeakingProgressEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun progressDao(): ProgressDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ielts_reading.db")
                // Version 1 was only used for testing before release, so it is simply rebuilt.
                // From now on, every version change needs a real Migration so students keep their progress.
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}
