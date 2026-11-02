package com.goal.ieltsreading.data.content

import android.content.Context
import com.goal.ieltsreading.data.db.AppDatabase
import com.goal.ieltsreading.data.db.Example
import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.LevelEntity
import com.goal.ieltsreading.data.db.QuestionEntity
import com.goal.ieltsreading.data.db.SentenceEntity
import com.goal.ieltsreading.data.db.WordForm
import com.goal.ieltsreading.data.db.WordPackEntity
import com.goal.ieltsreading.data.db.TestEntity
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.data.prefs.UserPrefs
import com.google.gson.Gson

/**
 * Copies the lessons from assets/content/ JSON files into the Room database.
 * It runs on the first launch and again whenever [CONTENT_VERSION] is increased.
 * User progress is stored in separate tables, so it is never lost.
 */
class ContentSeeder(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: UserPrefs
) {
    companion object {
        /** Increase this number every time you edit the JSON content. */
        const val CONTENT_VERSION = 2
        private const val CONTENT_DIR = "content"
        const val TEST_LEVEL_ID = "TEST"
        const val PACK_LEVEL_ID = "PACK"
    }

    private val gson = Gson()

    suspend fun seedIfNeeded() {
        if (prefs.contentVersion >= CONTENT_VERSION && db.contentDao().lessonCount() > 0) return
        seed()
        prefs.contentVersion = CONTENT_VERSION
    }

    private suspend fun seed() {
        val files = context.assets.list(CONTENT_DIR).orEmpty()
        val levelsJson = files
            .filter { it.startsWith("level_") && it.endsWith(".json") }
            .map { gson.fromJson(readAsset("$CONTENT_DIR/$it"), LevelJson::class.java) }
            .sortedBy { it.order }

        val levels = mutableListOf<LevelEntity>()
        val lessons = mutableListOf<LessonEntity>()
        val vocab = mutableListOf<VocabEntity>()
        val questions = mutableListOf<QuestionEntity>()
        var globalOrder = 0

        for (level in levelsJson) {
            levels += LevelEntity(
                id = level.id, sortOrder = level.order, title = level.title, titleGu = level.titleGu,
                band = level.band, description = level.description, descriptionGu = level.descriptionGu
            )
            for (lesson in level.lessons.sortedBy { it.order }) {
                lessons += LessonEntity(
                    id = lesson.id, levelId = level.id, sortOrder = lesson.order, globalOrder = globalOrder++,
                    title = lesson.title, titleGu = lesson.titleGu, skill = lesson.skill, minutes = lesson.minutes,
                    explanation = lesson.explanation, explanationGu = lesson.explanationGu,
                    examples = lesson.examples.orEmpty().map { Example(it.en, it.gu) },
                    passageTitle = lesson.passageTitle, passage = lesson.passage, passageGu = lesson.passageGu
                )
                vocab += lesson.vocabulary.orEmpty().map { it.toEntity(lesson.id, level.id) }
                questions += lesson.questions.orEmpty().mapIndexed { i, q -> q.toEntity(lesson.id, i) }
            }
        }

        val tests = mutableListOf<TestEntity>()
        if ("tests.json" in files) {
            val testsFile = gson.fromJson(readAsset("$CONTENT_DIR/tests.json"), TestsFileJson::class.java)
            for (t in testsFile.tests) {
                tests += TestEntity(
                    id = t.id, sortOrder = t.order, title = t.title, titleGu = t.titleGu, targetBand = t.targetBand,
                    requiredLevel = t.requiredLevel, minutes = t.minutes,
                    passageTitle = t.passageTitle, passage = t.passage
                )
                vocab += t.vocabulary.orEmpty().map { it.toEntity(t.id, TEST_LEVEL_ID) }
                questions += t.questions.orEmpty().mapIndexed { i, q -> q.toEntity(t.id, i) }
            }
        }

        val packs = mutableListOf<WordPackEntity>()
        if ("wordpacks.json" in files) {
            val file = gson.fromJson(readAsset("$CONTENT_DIR/wordpacks.json"), WordPacksFileJson::class.java)
            for (p in file.packs) {
                packs += WordPackEntity(p.id, p.order, p.title, p.titleGu, p.ielts, p.minLevel)
                vocab += p.words.orEmpty().mapIndexed { i, w -> w.toEntity(p.id, i) }
            }
        }

        val sentences = mutableListOf<SentenceEntity>()
        if ("sentences.json" in files) {
            val file = gson.fromJson(readAsset("$CONTENT_DIR/sentences.json"), SentencesFileJson::class.java)
            sentences += file.sentences.filter { it.focus != null && it.en.isNotBlank() }.map { s ->
                val focus = s.focus!!
                SentenceEntity(
                    id = s.id, level = s.level, pattern = s.pattern, en = s.en.trim(), gu = s.gu,
                    alternatives = s.alternatives.orEmpty(), tip = s.tip.orEmpty(), tipGu = s.tipGu.orEmpty(),
                    focusWord = focus.word, focusWrong = focus.wrong,
                    focusOptions = (focus.options.orEmpty() + focus.word + focus.wrong).distinct()
                )
            }
        }

        db.contentDao().replaceAll(levels, lessons, vocab.distinctBy { it.id }, questions, tests, packs, sentences)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun VocabJson.toEntity(ownerId: String, levelId: String) = VocabEntity(
        id = "$ownerId:${word.trim().lowercase()}",
        ownerId = ownerId,
        levelId = levelId,
        word = word.trim(),
        pos = pos.orEmpty(),
        meaningGu = meaningGu,
        meaningEn = meaningEn.orEmpty(),
        example = example.orEmpty(),
        exampleGu = exampleGu.orEmpty()
    )

    private fun PackWordJson.toEntity(packId: String, index: Int) = VocabEntity(
        id = "$packId:${word.trim().lowercase()}",
        ownerId = packId,
        levelId = PACK_LEVEL_ID,
        word = word.trim(),
        pos = pos.orEmpty(),
        meaningGu = meaningGu,
        meaningEn = meaningEn.orEmpty(),
        example = example.orEmpty(),
        exampleGu = exampleGu.orEmpty(),
        pronunciationGu = pronGu.orEmpty(),
        family = family.orEmpty().map { WordForm(it.word, it.pos, it.gu) },
        synonyms = synonyms.orEmpty(),
        opposites = opposites.orEmpty(),
        collocations = collocations.orEmpty(),
        ieltsTip = ieltsTip.orEmpty(),
        sortOrder = index
    )

    private fun QuestionJson.toEntity(ownerId: String, index: Int) = QuestionEntity(
        id = "$ownerId-q${index + 1}",
        ownerId = ownerId,
        sortOrder = index,
        type = type.uppercase(),
        prompt = prompt,
        promptGu = promptGu.orEmpty(),
        options = options.orEmpty(),
        answer = answer,
        alternatives = alternatives.orEmpty(),
        explanation = explanation.orEmpty(),
        explanationGu = explanationGu.orEmpty(),
        wordLimit = wordLimit.orEmpty()
    )
}
