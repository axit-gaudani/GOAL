package com.goal.ieltsreading.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        binding.textMessage.setText(if (passed) R.string.result_passed_msg else R.string.result_try_again_msg)

        val popToLevel = NavOptions.Builder().setPopUpTo(R.id.quizResultFragment, true).build()
        binding.btnRetry.setOnClickListener {
            findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to lessonId), popToLevel)
        }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNextLesson.visibleIf(false)

        if (passed) {
            viewLifecycleOwner.lifecycleScope.launch {
                val next = app.repository.lessonAfter(lessonId) ?: return@launch
                val b = _binding ?: return@launch
                b.btnNextLesson.visibleIf(true)
                b.btnNextLesson.text = getString(R.string.btn_next_lesson_format, next.lesson.title)
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
