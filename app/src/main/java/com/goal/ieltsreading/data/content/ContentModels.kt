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

// ---------------- Vocabulary builder: assets/content/wordpacks.json ----------------

data class WordPacksFileJson(val packs: List<WordPackJson> = emptyList())

data class WordPackJson(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val titleGu: String = "",
    val ielts: Boolean = false,
    val minLevel: Int = 0,
    val words: List<PackWordJson>? = null
)

data class PackWordJson(
    val word: String = "",
    val pos: String? = null,
    val pronGu: String? = null,
    val meaningGu: String = "",
    val meaningEn: String? = null,
    val example: String? = null,
    val exampleGu: String? = null,
    val family: List<WordFormJson>? = null,
    val synonyms: List<String>? = null,
    val opposites: List<String>? = null,
    val collocations: List<String>? = null,
    val ieltsTip: String? = null
)

data class WordFormJson(val word: String = "", val pos: String = "", val gu: String = "")

// ---------------- Sentence practice: assets/content/sentences.json ----------------

data class SentencesFileJson(val sentences: List<SentenceJson> = emptyList())

data class SentenceJson(
    val id: String = "",
    val level: Int = 0,
    val pattern: String = "",
    val en: String = "",
    val gu: String = "",
    val alternatives: List<String>? = null,
    val tip: String? = null,
    val tipGu: String? = null,
    val focus: SentenceFocusJson? = null
)

data class SentenceFocusJson(val word: String = "", val wrong: String = "", val options: List<String>? = null)

// ---------------- Basic speaking: assets/content/speaking.json (read directly, not stored in Room) ----------------

data class SpeakingFileJson(val partnerName: String = "Priya", val units: List<SpeakingUnit> = emptyList())

data class SpeakingUnit(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val titleGu: String = "",
    val phrases: List<SpeakingPhrase> = emptyList(),
    val conversation: List<ConversationTurn> = emptyList()
)

data class SpeakingPhrase(
    val en: String = "",
    val gu: String = "",
    val pronGu: String = "",
    val stress: List<String> = emptyList()
)

/** who = "partner" (has en/gu) or "you" (has options). */
data class ConversationTurn(
    val who: String = "partner",
    val en: String = "",
    val gu: String = "",
    val options: List<ConversationOption> = emptyList()
)

data class ConversationOption(val en: String = "", val gu: String = "")

// ---------------- Level −1 ABC: assets/content/abc.json (read directly, not stored in Room) ----------------

/** lessons: lesson id (e.g. "L-1-01") → the letters or sound groups taught in that lesson. */
data class AbcFileJson(val lessons: Map<String, List<AbcLetter>> = emptyMap())

data class AbcLetter(
    val upper: String = "",
    val lower: String = "",
    /** How the letter's name sounds, in Gujarati letters (B → બી). */
    val nameGu: String = "",
    /** The sound the letter usually makes, with an example. */
    val soundGu: String = "",
    val words: List<AbcWord> = emptyList()
)

data class AbcWord(val word: String = "", val gu: String = "", val pronGu: String = "")
