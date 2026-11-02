package com.goal.ieltsreading.data.content

/**
 * These classes mirror the JSON files in app/src/main/assets/content.
 * Gson fills them automatically, so the field names must match the JSON keys.
 * To add new lessons (for example from your own PDFs), just edit the JSON files
 * and increase ContentSeeder.CONTENT_VERSION.
 */
data class LevelJson(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val titleGu: String = "",
    val band: String = "",
    val description: String = "",
    val descriptionGu: String = "",
    val lessons: List<LessonJson> = emptyList()
)

data class LessonJson(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val titleGu: String = "",
    val skill: String = "",
    val minutes: Int = 10,
    val explanation: String = "",
    val explanationGu: String = "",
    val examples: List<ExampleJson>? = null,
    val passageTitle: String = "",
    val passage: String = "",
    val passageGu: String = "",
    val vocabulary: List<VocabJson>? = null,
    val questions: List<QuestionJson>? = null
)

data class ExampleJson(val en: String = "", val gu: String = "")

data class VocabJson(
    val word: String = "",
    val pos: String? = null,
    val meaningGu: String = "",
    val meaningEn: String? = null,
    val example: String? = null,
    val exampleGu: String? = null
)

data class QuestionJson(
    val type: String = "MCQ",
    val prompt: String = "",
    val promptGu: String? = null,
    val options: List<String>? = null,
    val answer: String = "",
    val alternatives: List<String>? = null,
    val explanation: String? = null,
    val explanationGu: String? = null,
    val wordLimit: String? = null
)

data class TestsFileJson(val tests: List<TestJson> = emptyList())

data class TestJson(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val titleGu: String = "",
    val targetBand: String = "",
    val requiredLevel: Int = 0,
    val minutes: Int = 20,
    val passageTitle: String = "",
    val passage: String = "",
    val vocabulary: List<VocabJson>? = null,
    val questions: List<QuestionJson>? = null
)
