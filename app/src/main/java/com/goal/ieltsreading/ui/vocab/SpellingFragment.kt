package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentSpellingBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf

class SpellingFragment : Fragment() {

    private var _binding: FragmentSpellingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpellingViewModel by viewModels()
    private var shownPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpellingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        shownPosition = -1
        binding.btnListen.setOnClickListener { viewModel.state.value?.word?.let { speak(it.word) } }
        binding.btnSlow.setOnClickListener { viewModel.state.value?.word?.let { speak(it.word, slow = true) } }
        binding.btnAction.setOnClickListener {
            val s = viewModel.state.value ?: return@setOnClickListener
            if (s.checked) viewModel.next() else viewModel.check(binding.inputAnswer.text?.toString().orEmpty())
        }
        binding.inputAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { binding.btnAction.performClick(); true } else false
        }
        binding.btnAgain.setOnClickListener { shownPosition = -1; viewModel.restart() }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.groupGame.visibleIf(!s.finished && s.word != null)
            binding.groupFinished.visibleIf(s.finished)
            binding.textEmpty.visibleIf(!s.finished && s.word == null)
            if (s.finished) {
                binding.textFinished.text = getString(R.string.spelling_finished, s.score, s.total)
                return@observe
            }
            val w = s.word ?: return@observe
            if (shownPosition != s.position) {
                shownPosition = s.position
                binding.inputAnswer.setText("")
                speak(w.word)
            }
            binding.textCounter.text = getString(R.string.counter_format, s.position, s.total)
            binding.textHint.text = getString(R.string.spelling_hint, w.meaningGu, w.word.length)
            binding.inputAnswer.isEnabled = !s.checked
            binding.cardFeedback.visibleIf(s.checked)
            if (s.checked) {
                binding.textFeedback.setText(if (s.correct) R.string.feedback_correct else R.string.feedback_wrong)
                binding.textFeedback.setTextColor(
                    ContextCompat.getColor(requireContext(), if (s.correct) R.color.success else R.color.error)
                )
                binding.textCorrectWord.text = w.word
                binding.textExample.text = w.example
            }
            binding.btnAction.setText(if (s.checked) R.string.btn_next_question else R.string.btn_check)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
