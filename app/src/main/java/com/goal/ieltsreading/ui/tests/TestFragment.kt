package com.goal.ieltsreading.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.db.QuestionEntity
import com.goal.ieltsreading.databinding.FragmentTestBinding
import com.goal.ieltsreading.databinding.ItemTestQuestionBinding
import com.goal.ieltsreading.domain.QuestionType
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton

/**
 * Timed practice test. Two buttons switch between the passage and the questions,
 * like moving between the text and the answer sheet in the real exam.
 */
class TestFragment : Fragment() {

    private var _binding: FragmentTestBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TestViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.leave_test_title)
                    .setMessage(R.string.leave_test_message)
                    .setNegativeButton(R.string.btn_stay, null)
                    .setPositiveButton(R.string.btn_leave) { _, _ ->
                        isEnabled = false
                        findNavController().navigateUp()
                    }
                    .show()
            }
        })

        binding.btnShowPassage.setOnClickListener { showPassage(true) }
        binding.btnShowQuestions.setOnClickListener { showPassage(false) }
        showPassage(true)

        binding.btnSubmit.setOnClickListener {
            val total = viewModel.questions.value.orEmpty().size
            val answered = viewModel.answers.size
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.submit_test_title)
                .setMessage(getString(R.string.submit_test_message, answered, total))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_submit) { _, _ -> viewModel.submit() }
                .show()
        }

        viewModel.test.observe(viewLifecycleOwner) { t ->
            if (t == null) return@observe
            binding.textPassageTitle.text = t.passageTitle
            binding.textPassage.text = t.passage
        }
        viewModel.questions.observe(viewLifecycleOwner) { renderQuestions(it) }
        viewModel.secondsLeft.observe(viewLifecycleOwner) { s ->
            binding.textTimer.text = getString(R.string.timer_format, s / 60, s % 60)
            val color = ContextCompat.getColor(requireContext(), if (s <= 120) R.color.error else R.color.g3_amber_text)
            binding.textTimer.setTextColor(color)
            binding.iconTimer.setColorFilter(color)
        }
        viewModel.answeredCount.observe(viewLifecycleOwner) { n ->
            binding.textAnswered.text = getString(R.string.g3_answered_format, n, viewModel.questions.value.orEmpty().size)
        }
        viewModel.submittedAttemptId.observe(viewLifecycleOwner) { id ->
            if (id == null) return@observe
            viewModel.submittedHandled()
            findNavController().navigate(
                R.id.testResultFragment, bundleOf("attemptId" to id),
                NavOptions.Builder().setPopUpTo(R.id.testFragment, true).build()
            )
        }
    }

    /** Passage / Questions switch: exactly one of the two buttons is selected. */
    private fun showPassage(passage: Boolean) {
        binding.btnShowPassage.isChecked = passage
        binding.btnShowQuestions.isChecked = !passage
        binding.scrollPassage.visibleIf(passage)
        binding.scrollQuestions.visibleIf(!passage)
    }

    private fun renderQuestions(questions: List<QuestionEntity>) {
        binding.questionsContainer.removeAllViews()
        val gap = (8 * resources.displayMetrics.density).toInt()
        questions.forEachIndexed { index, q ->
            val item = ItemTestQuestionBinding.inflate(layoutInflater, binding.questionsContainer, false)
            // Show an instruction header when a new question group starts (like the real exam),
            // with the question numbers it covers: "Questions 1–5 · True / False / Not Given".
            val startsGroup = index == 0 || questions[index - 1].type != q.type
            item.textGroupHeader.visibleIf(startsGroup)
            if (startsGroup) {
                var end = index
                while (end + 1 < questions.size && questions[end + 1].type == q.type) end++
                val label = QuestionType.label(q.type)
                item.textGroupHeader.text = if (end == index) getString(R.string.g3_question_single, index + 1, label)
                else getString(R.string.g3_questions_range, index + 1, end + 1, label)
            }
            item.textNumber.text = (index + 1).toString()
            item.textPrompt.text = q.prompt
            item.textWordLimit.text = q.wordLimit
            item.textWordLimit.visibleIf(q.type == QuestionType.FILL && q.wordLimit.isNotBlank())

            val saved = viewModel.answers[q.id]
            val isPills = q.type == QuestionType.TFNG || q.type == QuestionType.YNNG
            item.inputLayout.visibleIf(q.type == QuestionType.FILL)
            item.groupPills.visibleIf(isPills)
            item.radioGroup.visibleIf(!isPills && q.type != QuestionType.FILL)
            when {
                q.type == QuestionType.FILL -> {
                    item.inputAnswer.setText(saved.orEmpty())
                    item.inputAnswer.doAfterTextChanged { viewModel.setAnswer(q.id, it?.toString().orEmpty()) }
                }
                isPills -> {
                    val options = QuestionType.optionsFor(q)
                    val pills = options.mapIndexed { i, option ->
                        val pill = layoutInflater.inflate(R.layout.g3_item_pill_option, item.groupPills, false) as MaterialButton
                        pill.text = option
                        pill.isChecked = option == saved
                        if (i < options.lastIndex) {
                            (pill.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = gap
                        }
                        item.groupPills.addView(pill)
                        pill
                    }
                    pills.forEachIndexed { i, pill ->
                        pill.setOnClickListener {
                            pills.forEach { it.isChecked = it === pill }
                            viewModel.setAnswer(q.id, options[i])
                        }
                    }
                }
                else -> {
                    for (option in QuestionType.optionsFor(q)) {
                        val radio = MaterialRadioButton(requireContext())
                        radio.id = View.generateViewId()
                        radio.text = option
                        radio.textSize = 15f
                        radio.minHeight = (44 * resources.displayMetrics.density).toInt()
                        radio.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.g3_pill_stroke)
                        radio.isChecked = option == saved
                        radio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setAnswer(q.id, option) }
                        item.radioGroup.addView(radio)
                    }
                }
            }
            binding.questionsContainer.addView(item.root)
        }
        binding.textAnswered.text = getString(R.string.g3_answered_format, viewModel.answers.size, questions.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
