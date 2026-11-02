package com.goal.ieltsreading.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentHomeBinding
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
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = menuInflater.inflate(R.menu.menu_home, menu)
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId == R.id.action_settings) {
                    findNavController().navigate(R.id.settingsFragment); return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.cardLearningPath.setOnClickListener { findNavController().navigate(R.id.levelsFragment) }
        binding.btnReview.setOnClickListener { findNavController().navigate(R.id.flashcardFragment) }
        binding.cardTests.setOnClickListener { findNavController().navigate(R.id.testsFragment) }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textGreeting.text = greeting(s.userName)

            // Daily goal
            val t = s.today
            val goalPercent = if (t.goalMinutes == 0) 0 else (t.minutesToday * 100 / t.goalMinutes).coerceAtMost(100)
            binding.progressGoal.setProgressCompat(goalPercent, true)
            binding.textGoalPercent.text = getString(R.string.percent_format, goalPercent)
            binding.textGoalMinutes.text = getString(R.string.goal_minutes_format, t.minutesToday, t.goalMinutes)
            binding.textGoalStatus.text = getString(
                if (goalPercent >= 100) R.string.goal_done else R.string.goal_keep_going
            )
            binding.textStreak.text = getString(R.string.streak_format, t.streak)
            binding.textTodayStats.text = getString(R.string.today_stats_format, t.lessonsToday, t.wordsToday)

            // Continue learning
            val next = s.nextLesson
            if (next != null) {
                binding.textNextLevel.text = s.nextLessonLevelTitle
                binding.textNextTitle.text = next.lesson.title
                binding.textNextTitleGu.text = next.lesson.titleGu
                binding.textNextMeta.text = getString(R.string.lesson_meta_format, next.lesson.skill, next.lesson.minutes)
                binding.btnContinue.text = getString(
                    if ((next.progress?.attempts ?: 0) > 0) R.string.btn_try_again else R.string.btn_start_lesson
                )
                binding.btnContinue.setOnClickListener {
                    findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to next.lesson.id))
                }
            } else {
                binding.textNextLevel.text = getString(R.string.course_complete_title)
                binding.textNextTitle.text = getString(R.string.course_complete_text)
                binding.textNextTitleGu.text = ""
                binding.textNextMeta.text = ""
                binding.btnContinue.text = getString(R.string.btn_take_test)
                binding.btnContinue.setOnClickListener { findNavController().navigate(R.id.testsFragment) }
            }
            binding.progressCourse.setProgressCompat(s.coursePercent, true)
            binding.textCoursePercent.text = getString(R.string.course_progress_format, s.coursePercent)

            // Vocabulary review
            binding.textDueWords.text = resources.getQuantityString(R.plurals.due_words, s.dueWords, s.dueWords)
            binding.btnReview.isEnabled = s.dueWords > 0

            // Word of the day
            val w = s.wordOfDay
            binding.cardWordOfDay.visibleIf(w != null)
            if (w != null) {
                binding.textWodWord.text = w.word
                binding.textWodPos.text = w.pos
                binding.textWodMeaning.text = w.meaningGu
                binding.textWodExample.text = w.example
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
