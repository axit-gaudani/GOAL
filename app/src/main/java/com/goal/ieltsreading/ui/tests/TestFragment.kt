package com.goal.ieltsreading.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Timed practice test. A toggle switches between the passage and the questions,
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

        binding.toggleView.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.scrollPassage.visibleIf(checkedId == R.id.btnShowPassage)
            binding.scrollQuestions.visibleIf(checkedId == R.id.btnShowQuestions)
        }
        binding.toggleView.check(R.id.btnShowPassage)

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
            binding.textTimer.setTextColor(
                ContextCompat.getColor(requireContext(), if (s <= 120) R.color.error else R.color.brand_primary)
            )
        }
        viewModel.answeredCount.observe(viewLifecycleOwner) { n ->
            binding.textAnswered.text = getString(R.string.answered_format, n, viewModel.questions.value.orEmpty().size)
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

    private fun renderQuestions(questions: List<QuestionEntity>) {
        binding.questionsContainer.removeAllViews()
        var lastType = ""
        questions.forEachIndexed { index, q ->
            val item = ItemTestQuestionBinding.inflate(layoutInflater, binding.questionsContainer, false)
            // Show an instruction header when a new question group starts (like the real exam).
            item.textGroupHeader.visibleIf(q.type != lastType)
            item.textGroupHeader.text = QuestionType.label(q.type)
            lastType = q.type
            item.textNumber.text = (index + 1).toString()
            item.textPrompt.text = q.prompt
            item.textWordLimit.text = q.wordLimit
            item.textWordLimit.visibleIf(q.type == QuestionType.FILL && q.wordLimit.isNotBlank())

            val saved = viewModel.answers[q.id]
            if (q.type == QuestionType.FILL) {
                item.radioGroup.visibleIf(false)
                item.inputLayout.visibleIf(true)
                item.inputAnswer.setText(saved.orEmpty())
                item.inputAnswer.doAfterTextChanged { viewModel.setAnswer(q.id, it?.toString().orEmpty()) }
            } else {
                item.inputLayout.visibleIf(false)
                item.radioGroup.visibleIf(true)
                for (option in QuestionType.optionsFor(q)) {
                    val radio = RadioButton(requireContext())
                    radio.id = View.generateViewId()
                    radio.text = option
                    radio.isChecked = option == saved
                    radio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setAnswer(q.id, option) }
                    item.radioGroup.addView(radio)
                }
            }
            binding.questionsContainer.addView(item.root)
        }
        binding.textAnswered.text = getString(R.string.answered_format, viewModel.answers.size, questions.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
