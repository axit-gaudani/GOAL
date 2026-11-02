# Development guide (beginner friendly)

This guide explains how the app is built, in the order a beginner should read the code.

## Step 1 – The app starts: `IeltsApp.kt`

`IeltsApp` is created before any screen. It creates:

- `UserPrefs`: small settings such as name and daily goal (SharedPreferences),
- `AppDatabase`: the Room database,
- `LearningRepository`: the object every screen uses to read and save data.

This is simple manual dependency injection. Any Fragment can reach these objects with the `app` extension in
`ui/common/Extensions.kt`, and any ViewModel with `repository`.

## Step 2 – First launch: welcome screen

`MainActivity` checks `prefs.onboarded`. If it is false, it opens `WelcomeActivity`, which asks for:

- the student's name,
- a daily goal (10/15/20/30 minutes),
- "I already know basic English": turns on *open all levels*.

## Step 3 – Content: JSON → Room

`ContentSeeder` reads every `assets/content/level_*.json` file and `tests.json` with **Gson**, converts them to
Room entities and saves them in one transaction (`ContentDao.replaceAll`).

It runs the first time any repository function is called (`LearningRepository.ready()`), and again
when `CONTENT_VERSION` increases.

Important ids:

- Lesson: `L3-02` (level 3, lesson 2)
- Question: `L3-02-q1`
- Word: `L3-02:pollution`

Progress tables point to these ids, so keep them stable when you edit content.

## Step 4 – Navigation

`res/navigation/nav_graph.xml` lists every screen. The bottom bar (`res/menu/bottom_nav.xml`) uses the same ids
as the top-level destinations, so `setupWithNavController` connects them automatically.

To open a screen with data:

```kotlin
findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to lesson.id))
```

The ViewModel of that screen reads the value back:

```kotlin
class LessonViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val lessonId: String = handle["lessonId"] ?: ""
}
```

## Step 5 – A screen = Layout + Fragment + ViewModel

Example: `ui/home/`

1. `res/layout/fragment_home.xml` – the design.
2. `FragmentHomeBinding` – generated automatically by **ViewBinding** from that XML file.
3. `HomeViewModel` – loads data in `viewModelScope.launch { … }` and publishes it with `LiveData`.
4. `HomeFragment` – observes the LiveData and fills the views.

The standard ViewBinding pattern in a Fragment (it avoids memory leaks):

```kotlin
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...) = FragmentHomeBinding.inflate(inflater, container, false).also { _binding = it }.root
override fun onDestroyView() { super.onDestroyView(); _binding = null }
```

## Step 6 – Learning rules (`LearningRepository`)

| Rule | Where |
|---|---|
| A lesson unlocks when the previous lesson (in course order) is completed | `lessonItems()` |
| A lesson is passed at 60% (`PASS_PERCENT`) | `saveLessonResult()` |
| Passing a lesson adds its words to the review deck | `addLessonWordsToDeck()` |
| A test unlocks when its `requiredLevel` is reached | `tests()` |
| Streak = days in a row with study activity | `streak()` |
| Study time is counted while learning screens are open (max 30 min per visit) | `StudyTimeTracker` + `addStudySeconds()` |

## Step 7 – Domain logic (easy to unit-test)

- `AnswerChecker`: ignores case, extra spaces, end punctuation and a leading "the/a/an" for typed answers.
- `BandCalculator`: scales the score to 40 questions and applies the IELTS Academic Reading table.
- `SpacedRepetition`: Leitner boxes. A correct answer moves the word up one box (review after 1→2→4→7→15 days).
  A wrong answer sends it back to box 1.

Tests are in `app/src/test/.../DomainTest.kt`. Run them with `./gradlew testDebugUnitTest`.

## Step 8 – Daily reminder

`ReminderScheduler` schedules a `PeriodicWorkRequest` (WorkManager) every 24 hours at the time chosen in
Settings. `ReminderWorker` shows a notification only if today's goal is not complete. On Android 13+ the
Settings screen asks for the notification permission first.

## Ideas for next features

1. **Import from PDF**: use Android's `PdfRenderer` or a text-extraction library to turn your own PDFs into
   JSON lessons. Alternatively, convert PDFs to JSON on a computer and paste them into `assets/content`.
2. **Text-to-speech** (`android.speech.tts.TextToSpeech`) for word pronunciation.
3. **Full mock exam**: combine three tests into 60 minutes and 40 questions.
4. **Highlighting and notes** in passages, as in the computer-delivered IELTS.
5. **Cloud backup** of progress (Firebase or Google Drive).
6. **Room migrations**: when you change an entity, increase the database `version` and add a `Migration`, so
   students don't lose progress.
