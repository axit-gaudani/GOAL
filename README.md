# IELTS Reading Gujarati 📖

An Android app that teaches **IELTS Reading from Level 0 (absolute beginner) to Band 8+**, with Gujarati
explanations, Gujarati word meanings and step-by-step daily practice.

> શૂન્યથી IELTS Reading શીખો — સરળ અંગ્રેજી, ગુજરાતી સમજૂતી, રોજનો અભ્યાસ.

Built with **Kotlin + XML layouts + ViewBinding** (no Jetpack Compose), Room database, MVVM.

---

## 1. The learning path

The app never starts with difficult IELTS passages. Every lesson unlocks only after the previous one is
passed (60% or more), so a beginner moves up one small step at a time.

| Level | Name | Band / CEFR | Passage length | What is taught |
|---|---|---|---|---|
| −1 | ABC | Start | Letters and words | Letters A–Z with name and sound in Gujarati, blending (c-a-t), sh/ch/th, first sight words and tiny sentences, listening questions |
| 0 | Foundation | Pre-A1 | 45–60 words | Subject + Verb + Object, am/is/are, family/home/food words, question words |
| 1 | Beginner | A1 | ~110 words | Present simple, past simple, linking words, pronoun reference |
| 2 | Elementary | A2 | ~170 words | Main idea, scanning, synonyms and paraphrase, intro to TRUE/FALSE/NOT GIVEN |
| 3 | IELTS Band 5 | B1 | ~260 words | Skimming, TFNG traps, sentence completion, multiple choice |
| 4 | IELTS Band 6 | B2 | ~320 words | Relative clauses and passive, matching headings, YES/NO/NOT GIVEN, summary completion |
| 5 | IELTS Band 7 | C1 | ~410 words | Academic vocabulary, matching information, inference, time management |
| 6 | IELTS Band 8+ | C1+ | ~480 words | Complex arguments, writer's purpose and tone, expert trap-avoidance |

Then there are **5 timed IELTS-style practice tests** (Band 5 → Band 8+, 13 questions, 20 minutes each). Each
test unlocks when the student reaches the right level, and finishes with an **estimated band score**
(official 40-question conversion table) and a full answer review in English and Gujarati.

Total built-in content: 31 lessons (including 4 ABC lessons), 5 tests, 282 lesson questions and 247 vocabulary words with Gujarati meanings.
All passages are original.

## 2. Each lesson in 4 steps

1. **Learn** – the skill explained in simple English, plus a full Gujarati explanation and example sentences.
2. **Words** – 7–8 key words: Gujarati meaning, English meaning, example and its Gujarati translation.
3. **Read** – the passage. Key words are coloured: tap one to see its meaning. Show the Gujarati translation
   under each paragraph if you need it.
4. **Practice** – IELTS question types (multiple choice, TRUE/FALSE/NOT GIVEN, YES/NO/NOT GIVEN, sentence or
   summary completion). After each answer the app shows why it is right or wrong, in English and Gujarati. The
   passage stays one tap away.

## 3. Daily practice: sentences, vocabulary, speaking

| Feature | Where | What the learner does |
|---|---|---|
| **Sentence practice** | Home card, or Learn → Sentence practice | A round of 10 sentences a day, plus unlimited "10 more". Five exercise types: build the sentence, fill the gap, fix the mistake, listen and write, translate from Gujarati. Wrong sentences come back tomorrow, then after 3, 7, 15 and 30 days. Mastery is shown per grammar pattern (past simple, a/an, passive...). 250 sentences, Level 0 to Level 6. |
| **Vocabulary builder** | Words tab | 5 new words a day from a topic pack: sound in Gujarati letters, audio, word family, same/opposite meaning, collocations, IELTS tip. Review with spaced-repetition flashcards, plus Match pairs and Spelling games. 10 packs, 200 words (everyday packs first, IELTS and Academic Word List packs later). |
| **Basic speaking** | Speak tab | 7 units (introduce yourself → IELTS Speaking Part 1). Step 1: hear each phrase, see the stressed words, record your voice and compare, and let the phone's speech recognition check it. Step 2: a short conversation with Priya: choose an answer and say it. |

Audio uses the phone's built-in text-to-speech (works offline when an English voice is installed). The speech check
uses Android's speech recognition; on phones without it, learners can still record and compare their voice.

## 4. Other features

- **Home**: daily goal ring (minutes), streak 🔥, "continue learning" card, word of the day.
- **All my words**: search every word in English or Gujarati.
- **Progress**: streak, lessons, words mastered, best band, study time, a 7-day chart, progress per level and
  test history.
- **Settings**: name, daily goal, show Gujarati by default, "open all levels" for students who already know the
  basics, a daily reminder notification (only when today's goal is not complete) and a progress reset.
- Light and dark theme.

---

## 5. How to run the app (step by step)

1. Install **Android Studio** (Koala 2024.1 or newer). It includes JDK 17.
2. `File → Open…` and choose this folder (`GOAL`).
3. Wait for **Gradle sync** to finish. The first time, it downloads the libraries.
4. Create an emulator (`Device Manager → Create device → Pixel 7 → API 34`) or connect your phone with USB
   debugging on.
5. Press ▶ **Run**.

From the command line (macOS/Linux):

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 6. Project structure

```
app/src/main/
├── assets/content/            ← ALL learning content (JSON)
│   ├── level_-1.json … level_6.json   ← level_-1 = ABC
│   ├── abc.json               ← letters, sounds and example words for the ABC "Letters" tab
│   ├── tests.json
│   ├── sentences.json         ← Sentence practice
│   ├── wordpacks.json         ← Vocabulary builder packs
│   └── speaking.json          ← Basic speaking units
├── java/com/goal/ieltsreading/
│   ├── IeltsApp.kt            ← creates the database + repository once
│   ├── MainActivity.kt        ← toolbar + bottom navigation + NavHost
│   ├── data/
│   │   ├── content/           ← JSON models + ContentSeeder (JSON → Room)
│   │   ├── db/                ← Room entities, DAOs, AppDatabase
│   │   ├── prefs/UserPrefs.kt ← settings (SharedPreferences)
│   │   └── repo/              ← LearningRepository: the only place screens get data from
│   ├── domain/                ← pure logic: answer checking, band score, spaced repetition, dates
│   ├── audio/                 ← text-to-speech (Speaker) and voice recording
│   ├── reminder/              ← WorkManager daily reminder
│   └── ui/                    ← one package per screen (Fragment + ViewModel + Adapter)
│       ├── welcome/ home/ learn/ lesson/ quiz/ sentence/ vocab/ speak/ tests/ progress/ settings/ common/
└── res/
    ├── layout/                ← XML layouts (ViewBinding classes are generated from these)
    ├── navigation/nav_graph.xml
    └── values/                ← strings, colors, themes (values-night = dark mode)
```

### Architecture (MVVM)

```
Fragment (XML + ViewBinding)  ──observes LiveData──▶  ViewModel  ──suspend calls──▶  LearningRepository
                                                                                     │
                                                         Room database ◀─────────────┤
                                                         assets/content/*.json ──────┘ (seeded once)
```

- **Fragments** only show data and forward clicks.
- **ViewModels** (`AndroidViewModel`) hold screen state and survive screen rotation. Screen arguments
  (`lessonId`, `testId`…) arrive through `SavedStateHandle`.
- **LearningRepository** contains the learning rules: unlocking, the pass mark, streaks, the review deck.
- **Room** stores two kinds of tables:
  - *content* tables (`levels`, `lessons`, `vocabulary`, `questions`, `practice_tests`) are rebuilt from JSON,
  - *progress* tables (`lesson_progress`, `vocab_progress`, `test_attempts`, `daily_activity`) are never
    deleted when content changes, so student progress is safe.

---

## 7. Adding your own content (for example from your PDFs)

All lessons live in `app/src/main/assets/content/`. You don't need to change any Kotlin code to add content.

1. Open a level file, for example `level_3.json`, and copy an existing lesson object.
2. Give it a new unique `id` (for example `"L3-05"`) and the next `order`.
3. Fill in `explanation` / `explanationGu`, `passage` / `passageGu` (separate paragraphs with `\n\n`),
   `vocabulary` and `questions`.
4. Open `data/content/ContentSeeder.kt` and **increase `CONTENT_VERSION` by 1**.
5. Run the app. The new content is loaded and existing progress is kept.

Question formats:

```json
{"type": "MCQ",  "prompt": "...", "options": ["A", "B", "C", "D"], "answer": "B", "explanation": "...", "explanationGu": "..."}
{"type": "TFNG", "prompt": "statement", "answer": "NOT GIVEN", "explanation": "...", "explanationGu": "..."}
{"type": "YNNG", "prompt": "writer's claim", "answer": "YES", "explanation": "...", "explanationGu": "..."}
{"type": "FILL", "prompt": "Bees visit ____ flowers.", "answer": "wild", "alternatives": ["wild flowers"], "wordLimit": "ONE WORD ONLY"}
```

New tests go into `tests.json`. Set `requiredLevel` to the level a student must reach first.

> Only add text you have the right to use. Published IELTS books (such as Cambridge IELTS) are copyrighted, so
> use them for your own study, not as app content you share with others.

See [`docs/DEVELOPMENT_GUIDE.md`](docs/DEVELOPMENT_GUIDE.md) for a step-by-step explanation of how the code
works and ideas for next features.
