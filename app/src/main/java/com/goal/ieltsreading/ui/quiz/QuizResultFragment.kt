package com.goal.ieltsreading.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.LearningRepository
import com.goal.ieltsreading.databinding.FragmentQuizResultBinding
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.visibleIf
import kotlinx.coroutines.launch

class QuizResultFragment : Fragment() {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val lessonId = args.getString("lessonId").orEmpty()
        val correct = args.getInt("correct")
        val total = args.getInt("total")
        val percent = if (total == 0) 100 else correct * 100 / total
        val passed = percent >= LearningRepository.PASS_PERCENT

        binding.progressScore.setProgressCompat(percent, true)
        binding.textPercent.text = getString(R.string.percent_format, percent)
        binding.textScore.text = getString(R.string.score_format, correct, total)
        binding.textTitle.setText(
            when {
                percent == 100 -> R.string.result_perfect
                passed -> R.string.result_passed
                else -> R.string.result_try_again
            }
        )
        binding.textMessage.text =
            if (passed) getString(R.string.g2_result_passed_gu)
            else getString(R.string.g2_result_try_again_gu, LearningRepository.PASS_PERCENT)
        // Ring: green when passed, amber when not yet.
        binding.progressScore.setIndicatorColor(
            ContextCompat.getColor(requireContext(), if (passed) R.color.g2_correct else R.color.brand_secondary)
        )
        binding.textStatCorrect.text = getString(R.string.g2_done_of_total, correct, total)

        val popToLevel = NavOptions.Builder().setPopUpTo(R.id.quizResultFragment, true).build()
        binding.btnRetry.setOnClickListener {
            findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to lessonId), popToLevel)
        }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNextLesson.visibleIf(false)
        binding.cardUnlocked.visibleIf(false)

        // Stats the app really has: words taught in this lesson and the best score so far.
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = app.repository
            val wordCount = repo.vocabulary(lessonId).size
            val best = repo.lessonProgress(lessonId)?.bestScore
            val b = _binding ?: return@launch
            b.textStatNewWords.text = wordCount.toString()
            b.tileNewWords.visibleIf(wordCount > 0)
            b.textStatBest.text = getString(R.string.percent_format, best ?: percent)
            b.tileBest.visibleIf(best != null)
        }

        if (passed) {
            viewLifecycleOwner.lifecycleScope.launch {
                val next = app.repository.lessonAfter(lessonId) ?: return@launch
                val b = _binding ?: return@launch
                b.btnNextLesson.visibleIf(true)
                b.cardUnlocked.visibleIf(true)
                b.textUnlocked.text = getString(R.string.g2_unlocked_format, next.lesson.title)
                b.btnNextLesson.setOnClickListener {
                    findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to next.lesson.id), popToLevel)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
