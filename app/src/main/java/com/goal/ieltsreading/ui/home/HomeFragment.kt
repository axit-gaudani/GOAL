package com.goal.ieltsreading.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.content.ContentSeeder
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.goal.ieltsreading.databinding.FragmentHomeBinding
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.goal.ieltsreading.ui.vocab.WordSheet
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.settingsFragment) }

        binding.cardLearningPath.setOnClickListener { findNavController().navigate(R.id.levelsFragment) }
        binding.cardReview.setOnClickListener { findNavController().navigate(R.id.flashcardFragment) }
        // "Practise sounds" opens Level −1 (ABC: letters and sounds).
        binding.cardSounds.setOnClickListener {
            findNavController().navigate(R.id.levelDetailFragment, bundleOf("levelId" to ContentSeeder.ABC_LEVEL_ID))
        }
        binding.cardTests.setOnClickListener { findNavController().navigate(R.id.testsFragment) }
        binding.cardSentences.setOnClickListener { findNavController().navigate(R.id.sentenceHubFragment) }
        // Speak is a bottom tab, so switch tabs instead of opening it on top of Home.
        binding.cardSpeaking.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.speakHubFragment
        }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textGreeting.text = greeting(s.userName)

            // Daily goal
            val t = s.today
            val goalPercent = if (t.goalMinutes == 0) 0 else (t.minutesToday * 100 / t.goalMinutes).coerceAtMost(100)
            binding.progressGoal.setProgressCompat(goalPercent, true)
            binding.textGoalPercent.text = getString(R.string.percent_format, goalPercent)
            binding.textGoalMinutes.text = getString(R.string.g1_goal_minutes_format, t.minutesToday, t.goalMinutes)
            binding.textStreak.text = getString(R.string.g1_streak_format, t.streak)

            // Continue learning
            val next = s.nextLesson
            if (next != null) {
                binding.textNextLevel.text = if (s.nextLessonLevelTitle.isBlank())
                    getString(R.string.g1_continue_label)
                else getString(R.string.g1_continue_level_format, s.nextLessonLevelTitle)
                binding.textNextTitle.text = next.lesson.title
                binding.textNextTitleGu.text = next.lesson.titleGu
                binding.textNextTitleGu.visibleIf(next.lesson.titleGu.isNotBlank())
                val action = getString(
                    if ((next.progress?.attempts ?: 0) > 0) R.string.btn_try_again else R.string.btn_start_lesson
                )
                binding.btnContinue.text = getString(R.string.g1_btn_with_minutes, action, next.lesson.minutes)
                binding.btnContinue.setOnClickListener {
                    findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to next.lesson.id))
                }
            } else {
                binding.textNextLevel.text = getString(R.string.course_complete_title)
                binding.textNextTitle.text = getString(R.string.course_complete_text)
                binding.textNextTitleGu.text = ""
                binding.textNextTitleGu.visibleIf(false)
                binding.btnContinue.text = getString(R.string.btn_take_test)
                binding.btnContinue.setOnClickListener { findNavController().navigate(R.id.testsFragment) }
            }
            binding.progressCourse.setProgressCompat(s.coursePercent, true)
            binding.textCoursePercent.text = getString(R.string.g1_course_percent_format, s.coursePercent)

            binding.textSentencesSub.text = if (s.dueSentences > 0)
                getString(R.string.sentences_due_format, s.dueSentences) else getString(R.string.sentences_card_sub)

            // Vocabulary review
            binding.textDueWords.text = if (s.dueWords > 0)
                resources.getQuantityString(R.plurals.g1_words_to_review, s.dueWords, s.dueWords)
            else getString(R.string.g1_no_words_to_review)
            binding.cardReview.isEnabled = s.dueWords > 0
            binding.cardReview.alpha = if (s.dueWords > 0) 1f else 0.6f

            // Word of the day
            val w = s.wordOfDay
            binding.cardWordOfDay.visibleIf(w != null)
            if (w != null) {
                binding.textWodWord.text = w.word
                binding.textWodPron.text = s.wordOfDayPronGu
                binding.textWodPron.visibleIf(s.wordOfDayPronGu.isNotBlank())
                binding.textWodMeaning.text = w.meaningGu
                binding.btnWodPlay.setOnClickListener { speak(w.word) }
                binding.cardWordOfDay.setOnClickListener {
                    WordSheet.newInstance(w.id, w.word, w.pos, w.meaningGu, w.meaningEn, w.example, w.exampleGu)
                        .show(childFragmentManager, "word")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun greeting(name: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val base = getString(
            when {
                hour < 12 -> R.string.good_morning
                hour < 17 -> R.string.good_afternoon
                else -> R.string.good_evening
            }
        )
        return if (name.isBlank()) base else "$base, $name"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
