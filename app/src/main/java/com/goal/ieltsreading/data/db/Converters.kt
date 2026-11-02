package com.goal.ieltsreading.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Room can only store simple values, so lists are saved as JSON text. */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun stringListToJson(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter
    fun exampleListToJson(value: List<Example>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToExampleList(value: String): List<Example> =
        gson.fromJson(value, object : TypeToken<List<Example>>() {}.type) ?: emptyList()
}
