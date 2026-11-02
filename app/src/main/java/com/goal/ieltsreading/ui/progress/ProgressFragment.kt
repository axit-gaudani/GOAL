package com.goal.ieltsreading.ui.progress

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentProgressBinding
import com.goal.ieltsreading.databinding.ItemAttemptBinding
import com.goal.ieltsreading.databinding.ItemLevelProgressBinding
import com.goal.ieltsreading.domain.BandCalculator
import com.goal.ieltsreading.ui.common.visibleIf
import java.util.Date

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.stats.observe(viewLifecycleOwner) { s ->
            binding.textStreak.text = s.streak.toString()
            binding.textLessons.text = getString(R.string.fraction_format, s.lessonsCompleted, s.totalLessons)
            binding.textWords.text = getString(R.string.fraction_format, s.wordsMastered, s.wordsInDeck)
            binding.textBestBand.text = s.bestBand?.let { BandCalculator.format(it) } ?: "–"
            binding.textTotalTime.text = getString(R.string.total_time_format, s.totalMinutes / 60, s.totalMinutes % 60)
            binding.textAvgScore.text = getString(R.string.percent_format, s.averageLessonScore)
            binding.chartWeek.setData(s.week, viewModel.goalMinutes)
            binding.textWeekGoal.text = getString(R.string.week_goal_format, s.week.count { it.reachedGoal })

            binding.levelsContainer.removeAllViews()
            for (l in s.levels) {
                val item = ItemLevelProgressBinding.inflate(layoutInflater, binding.levelsContainer, false)
                item.textName.text = getString(R.string.level_title_format, l.level.sortOrder, l.level.title)
                item.textCount.text = getString(R.string.fraction_format, l.completedLessons, l.totalLessons)
                item.progress.setProgressCompat(l.percent, false)
                binding.levelsContainer.addView(item.root)
            }

            binding.attemptsContainer.removeAllViews()
            binding.textNoAttempts.visibleIf(s.recentAttempts.isEmpty())
            for ((attempt, title) in s.recentAttempts) {
                val item = ItemAttemptBinding.inflate(layoutInflater, binding.attemptsContainer, false)
                item.textTitle.text = title
                item.textDate.text = DateFormat.getMediumDateFormat(requireContext()).format(Date(attempt.takenAt))
                item.textScore.text = getString(R.string.score_format, attempt.correct, attempt.total)
                item.textBand.text = BandCalculator.format(attempt.band)
                binding.attemptsContainer.addView(item.root)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
