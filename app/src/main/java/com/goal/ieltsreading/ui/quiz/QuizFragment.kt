package com.goal.ieltsreading.ui.quiz

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.button.MaterialButton

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private var renderedQuestionId: String? = null
    private var defaultActionTint: ColorStateList? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        renderedQuestionId = null
        defaultActionTint = binding.btnAction.backgroundTintList

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
        // LISTEN questions hide the text until the answer is checked; the learner hears it instead.
        val isListen = q.type == QuestionType.LISTEN
        binding.textPrompt.text = q.prompt
        binding.textPrompt.visibleIf(!isListen || s.checked)
        binding.textPromptGu.text = q.promptGu
        binding.textPromptGu.visibleIf((showGu || isListen) && q.promptGu.isNotBlank())
        binding.groupListen.visibleIf(isListen)
        binding.btnListenQuestion.setOnClickListener { speak(q.prompt) }
        binding.btnListenSlow.setOnClickListener { speak(q.prompt, slow = true) }
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
            if (isListen) speak(q.prompt)
        }
        binding.inputAnswer.isEnabled = !s.checked
        styleOptions(s)

        // Feedback: a coloured bottom panel (green when right, red when wrong) with the Next button inside.
        val ctx = requireContext()
        fun color(res: Int) = ContextCompat.getColor(ctx, res)
        binding.cardFeedback.visibleIf(s.checked)
        (binding.panelFeedback.background.mutate() as? GradientDrawable)?.setColor(
            if (!s.checked) Color.TRANSPARENT
            else color(if (s.correct) R.color.g2_correct_soft else R.color.error_container)
        )
        if (s.checked) {
            binding.textFeedbackTitle.setText(if (s.correct) R.string.feedback_correct else R.string.feedback_wrong)
            binding.textFeedbackTitle.setTextColor(color(if (s.correct) R.color.g2_correct_text else R.color.g2_wrong_text))
            binding.textCorrectAnswer.text = getString(R.string.correct_answer_format, q.answer)
            binding.textExplanation.text = q.explanation
            binding.textExplanation.visibleIf(q.explanation.isNotBlank())
            binding.textExplanationGu.text = q.explanationGu
            binding.textExplanationGu.visibleIf(q.explanationGu.isNotBlank())
            binding.btnAction.backgroundTintList =
                ColorStateList.valueOf(color(if (s.correct) R.color.g2_correct else R.color.error))
        } else {
            binding.btnAction.backgroundTintList = defaultActionTint
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

    /** Option states as in the design: selected = blue, correct = green + tick, wrong = red + cross. */
    private fun styleOptions(s: QuizState) {
        val ctx = requireContext()
        fun color(res: Int) = ContextCompat.getColor(ctx, res)
        val density = resources.displayMetrics.density
        for (i in 0 until binding.optionsContainer.childCount) {
            val button = binding.optionsContainer.getChildAt(i) as MaterialButton
            val option = button.tag as String
            val isSelected = option == s.answer
            val isAnswer = option.equals(s.question.answer, ignoreCase = true)
            // Border, background and text colours for this option
            val (stroke, bg, text) = when {
                s.checked && isAnswer -> Triple(R.color.g2_correct, R.color.g2_correct_soft, R.color.g2_correct_text)
                s.checked && isSelected -> Triple(R.color.error, R.color.error_container, R.color.g2_wrong_text)
                s.checked -> Triple(R.color.track, R.color.surface, R.color.text_secondary)
                isSelected -> Triple(R.color.brand_primary, R.color.brand_primary_container, R.color.brand_on_primary_container)
                else -> Triple(R.color.track, R.color.surface, R.color.text_primary)
            }
            val strong = isSelected || (s.checked && isAnswer)
            button.strokeColor = ColorStateList.valueOf(color(stroke))
            button.strokeWidth = ((if (strong) 2.5f else 1.5f) * density).toInt()
            button.backgroundTintList = ColorStateList.valueOf(color(bg))
            button.setTextColor(color(text))
            button.typeface = ResourcesCompat.getFont(ctx, if (strong) R.font.inter_bold else R.font.inter_semibold)
            val icon = when {
                s.checked && isAnswer -> R.drawable.ic_g2_check
                s.checked && isSelected -> R.drawable.ic_g2_x
                else -> 0
            }
            if (icon == 0) {
                button.icon = null
            } else {
                button.setIconResource(icon)
                button.iconTint = ColorStateList.valueOf(color(text))
            }
            button.isEnabled = !s.checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
