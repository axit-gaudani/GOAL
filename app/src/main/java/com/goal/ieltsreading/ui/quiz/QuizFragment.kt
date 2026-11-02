package com.goal.ieltsreading.ui.quiz

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentQuizBinding
import com.goal.ieltsreading.domain.QuestionType
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.button.MaterialButton

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private var renderedQuestionId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        renderedQuestionId = null

        binding.btnPassage.setOnClickListener {
            val lesson = viewModel.lesson.value ?: return@setOnClickListener
            PassageSheet.newInstance(lesson.passageTitle, lesson.passage).show(childFragmentManager, "passage")
        }
        binding.btnAction.setOnClickListener {
            val state = viewModel.state.value ?: return@setOnClickListener
            if (state.checked) viewModel.next()
            else viewModel.check(if (state.question.type == QuestionType.FILL) binding.inputAnswer.text?.toString() else null)
        }
        binding.inputAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { binding.btnAction.performClick(); true } else false
        }

        viewModel.state.observe(viewLifecycleOwner) { s -> if (s != null) render(s) }
        viewModel.finished.observe(viewLifecycleOwner) { f ->
            if (f == null) return@observe
            viewModel.finishedHandled()
            findNavController().navigate(
                R.id.quizResultFragment,
                bundleOf("lessonId" to viewModel.lessonId, "correct" to f.correct, "total" to f.total),
                NavOptions.Builder().setPopUpTo(R.id.lessonFragment, true).build()
            )
        }
    }

    private fun render(s: QuizState) {
        val q = s.question
        val showGu = viewModel.showGujarati
        binding.progress.max = s.total
        binding.progress.setProgressCompat(s.index + if (s.checked) 1 else 0, true)
        binding.textCounter.text = getString(R.string.question_counter, s.index + 1, s.total)
        binding.textType.text = QuestionType.label(q.type)
        binding.textTypeGu.text = QuestionType.labelGu(q.type)
        binding.textTypeGu.visibleIf(showGu)
        binding.textPrompt.text = q.prompt
        binding.textPromptGu.text = q.promptGu
        binding.textPromptGu.visibleIf(showGu && q.promptGu.isNotBlank())
        binding.textWordLimit.text = q.wordLimit
        binding.textWordLimit.visibleIf(q.type == QuestionType.FILL && q.wordLimit.isNotBlank())

        // Answer area
        val isFill = q.type == QuestionType.FILL
        binding.inputLayout.visibleIf(isFill)
        binding.optionsContainer.visibleIf(!isFill)
        if (renderedQuestionId != q.id) {
            renderedQuestionId = q.id
            binding.inputAnswer.setText("")
            binding.optionsContainer.removeAllViews()
            for (option in QuestionType.optionsFor(q)) {
                val button = layoutInflater.inflate(R.layout.view_option_button, binding.optionsContainer, false) as MaterialButton
                button.text = option
                button.tag = option
                button.setOnClickListener { viewModel.select(option) }
                binding.optionsContainer.addView(button)
            }
            binding.scroll.scrollTo(0, 0)
        }
        binding.inputAnswer.isEnabled = !s.checked
        styleOptions(s)

        // Feedback
        binding.cardFeedback.visibleIf(s.checked)
        if (s.checked) {
            binding.textFeedbackTitle.setText(if (s.correct) R.string.feedback_correct else R.string.feedback_wrong)
            binding.textFeedbackTitle.setTextColor(
                ContextCompat.getColor(requireContext(), if (s.correct) R.color.success else R.color.error)
            )
            binding.textCorrectAnswer.text = getString(R.string.correct_answer_format, q.answer)
            binding.textExplanation.text = q.explanation
            binding.textExplanation.visibleIf(q.explanation.isNotBlank())
            binding.textExplanationGu.text = q.explanationGu
            binding.textExplanationGu.visibleIf(q.explanationGu.isNotBlank())
        }
        binding.btnAction.setText(
            when {
                !s.checked -> R.string.btn_check
                s.index + 1 >= s.total -> R.string.btn_see_result
                else -> R.string.btn_next_question
            }
        )
        binding.btnAction.isEnabled = s.checked || isFill || s.answer != null
    }

    private fun styleOptions(s: QuizState) {
        val ctx = requireContext()
        for (i in 0 until binding.optionsContainer.childCount) {
            val button = binding.optionsContainer.getChildAt(i) as MaterialButton
            val option = button.tag as String
            val isSelected = option == s.answer
            val isAnswer = option.equals(s.question.answer, ignoreCase = true)
            val colorRes = when {
                s.checked && isAnswer -> R.color.success
                s.checked && isSelected -> R.color.error
                isSelected -> R.color.brand_primary
                else -> R.color.option_stroke
            }
            val color = ContextCompat.getColor(ctx, colorRes)
            button.strokeColor = ColorStateList.valueOf(color)
            button.strokeWidth = resources.getDimensionPixelSize(if (isSelected || (s.checked && isAnswer)) R.dimen.stroke_selected else R.dimen.stroke_normal)
            button.isEnabled = !s.checked
            button.alpha = if (s.checked && !isAnswer && !isSelected) 0.6f else 1f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
