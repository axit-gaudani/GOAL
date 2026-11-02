package com.goal.ieltsreading.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LevelEntity::class, LessonEntity::class, VocabEntity::class, QuestionEntity::class, TestEntity::class,
        LessonProgressEntity::class, VocabProgressEntity::class, TestAttemptEntity::class, DailyActivityEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun progressDao(): ProgressDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ielts_reading.db")
                // When you change the database version later, add a Migration here
                // so that the student's progress is kept.
                .build()
    }
}
