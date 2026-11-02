package com.goal.ieltsreading.ui.sentence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.SentenceExercise
import com.goal.ieltsreading.databinding.FragmentSentenceRoundBinding
import com.goal.ieltsreading.databinding.ItemMissedSentenceBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** One round of 10 sentences. Each sentence uses one exercise type; the result shows at the end. */
class SentenceRoundFragment : Fragment() {

    private var _binding: FragmentSentenceRoundBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SentenceRoundViewModel by viewModels()
    private var shownIndex = -1
    private var spokenIndex = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSentenceRoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        shownIndex = -1
        spokenIndex = -1

        binding.btnClose.setOnClickListener { findNavController().navigateUp() }
        binding.inputAnswer.doAfterTextChanged { refreshButton() }
        binding.btnListen.setOnClickListener { viewModel.state.value?.let { speak(it.item.sentence.en) } }
        binding.btnListenSlow.setOnClickListener { viewModel.state.value?.let { speak(it.item.sentence.en, slow = true) } }
        binding.btnPlayAnswer.setOnClickListener { viewModel.state.value?.let { speak(it.item.sentence.en) } }
        binding.btnAction.setOnClickListener {
            val s = viewModel.state.value ?: return@setOnClickListener
            if (s.checked) viewModel.next() else viewModel.check(binding.inputAnswer.text?.toString().orEmpty())
        }
        binding.btnMore.setOnClickListener { shownIndex = -1; spokenIndex = -1; viewModel.newRound() }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }

        viewModel.state.observe(viewLifecycleOwner) { s -> if (s != null) render(s) }
        viewModel.result.observe(viewLifecycleOwner) { r ->
            binding.groupExercise.visibleIf(r == null)
            binding.groupResult.visibleIf(r != null)
            if (r == null) return@observe
            binding.textResultScore.text = getString(R.string.fraction_format, r.correct, r.total)
            binding.textResultTime.text = getString(R.string.time_taken_format, r.seconds / 60, r.seconds % 60)
            binding.textResultEmpty.visibleIf(r.total == 0)
            binding.textMissedHeader.visibleIf(r.missed.isNotEmpty())
            binding.missedContainer.removeAllViews()
            for (m in r.missed) {
                val item = ItemMissedSentenceBinding.inflate(layoutInflater, binding.missedContainer, false)
                item.textCorrect.text = m.en
                item.textGiven.text = getString(R.string.you_wrote_format, m.given.ifBlank { "—" })
                item.textTip.text = m.tipGu
                item.textTip.visibleIf(m.tipGu.isNotBlank())
                item.btnPlay.setOnClickListener { speak(m.en) }
                binding.missedContainer.addView(item.root)
            }
        }
    }

    private fun render(s: RoundState) {
        val item = s.item
        val sentence = item.sentence
        val type = item.exercise
        val newSentence = shownIndex != s.index
        shownIndex = s.index

        binding.progress.max = s.total
        binding.progress.setProgressCompat(s.index + if (s.checked) 1 else 0, true)
        binding.textCounter.text = getString(R.string.counter_format, s.index + 1, s.total)
        binding.textType.text = getString(R.string.exercise_label_format, type.title, sentence.pattern)
        binding.textRepeat.visibleIf(item.isRepeat)
        binding.textInstruction.text = getString(instruction(type))
        binding.textInstructionGu.text = getString(instructionGu(type))

        // Prompt
        val showGujarati = type == SentenceExercise.BUILD || type == SentenceExercise.TRANSLATE
        binding.textPromptGu.visibleIf(showGujarati)
        binding.textPromptGu.text = sentence.gu
        binding.groupListen.visibleIf(type == SentenceExercise.DICTATION)
        binding.wordsGroup.visibleIf(type == SentenceExercise.FILL || type == SentenceExercise.FIX)
        if (type == SentenceExercise.FILL) {
            fillChips(binding.wordsGroup, viewModel.fillWords(item, if (s.chosen != null) s.chosen else "____"), -1, null)
        } else if (type == SentenceExercise.FIX) {
            fillChips(binding.wordsGroup, viewModel.fixWords(item), s.tappedWrong) { viewModel.tapWord(it) }
        }

        // Answer area
        binding.groupBuild.visibleIf(type == SentenceExercise.BUILD)
        if (type == SentenceExercise.BUILD) {
            tileChips(binding.answerGroup, s.answer.map { it.text }, filled = true) { i -> viewModel.tapAnswer(s.answer[i]) }
            tileChips(binding.bankGroup, s.bank.map { it.text }, filled = false) { i -> viewModel.tapBank(s.bank[i]) }
        }
        val needsOptions = type == SentenceExercise.FILL || (type == SentenceExercise.FIX && s.tappedWrong >= 0)
        binding.textOptionsHeader.visibleIf(needsOptions)
        binding.optionsGroup.visibleIf(needsOptions)
        if (needsOptions) {
            optionChips(binding.optionsGroup, sentence.focusOptions.filter { it != sentence.focusWrong || type == SentenceExercise.FILL }, s)
        }
        val typing = type == SentenceExercise.DICTATION || type == SentenceExercise.TRANSLATE
        binding.inputLayout.visibleIf(typing)
        if (newSentence) {
            binding.inputAnswer.setText("")
            binding.scroll.scrollTo(0, 0)
            if (type == SentenceExercise.DICTATION) speak(sentence.en)
        }
        binding.inputAnswer.isEnabled = !s.checked

        // Feedback
        binding.cardFeedback.visibleIf(s.checked)
        if (s.checked) {
            binding.cardFeedback.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), if (s.correct) R.color.success_container else R.color.error_container)
            )
            val color = ContextCompat.getColor(requireContext(), if (s.correct) R.color.success else R.color.error)
            binding.textFeedbackTitle.setText(if (s.correct) R.string.feedback_correct else R.string.feedback_wrong)
            binding.textFeedbackTitle.setTextColor(color)
            binding.textCorrectSentence.text = sentence.en
            binding.textCorrectGu.text = sentence.gu
            binding.textTip.text = sentence.tipGu.ifBlank { sentence.tip }
            binding.textTip.visibleIf(binding.textTip.text.isNotBlank())
            if (spokenIndex != s.index) {
                spokenIndex = s.index
                speak(sentence.en)
            }
        }
        binding.btnAction.setText(
            when {
                !s.checked -> R.string.btn_check
                s.index + 1 >= s.total -> R.string.btn_see_result
                else -> R.string.btn_next_sentence
            }
        )
        refreshButton()
    }

    private fun refreshButton() {
        val s = viewModel.state.value ?: return
        _binding?.btnAction?.isEnabled = s.checked || viewModel.canCheck(binding.inputAnswer.text?.toString().orEmpty())
    }

    /** Sentence words as chips; [onTap] makes them tappable (FIX). */
    private fun fillChips(group: ChipGroup, words: List<String>, selected: Int, onTap: ((Int) -> Unit)?) {
        group.removeAllViews()
        words.forEachIndexed { i, w ->
            val chip = Chip(requireContext())
            chip.text = w
            chip.textSize = 18f
            chip.isClickable = onTap != null
            chip.isCheckable = false
            if (i == selected) {
                chip.setChipBackgroundColorResource(R.color.brand_secondary_container)
                chip.setChipStrokeColorResource(R.color.brand_secondary)
                chip.chipStrokeWidth = resources.displayMetrics.density * 2
            }
            if (onTap != null) chip.setOnClickListener { onTap(i) }
            group.addView(chip)
        }
    }

    private fun tileChips(group: ChipGroup, words: List<String>, filled: Boolean, onTap: (Int) -> Unit) {
        group.removeAllViews()
        words.forEachIndexed { i, w ->
            val chip = Chip(requireContext())
            chip.text = w
            chip.textSize = 18f
            chip.isCheckable = false
            if (filled) {
                chip.setChipBackgroundColorResource(R.color.brand_primary)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            chip.setOnClickListener { onTap(i) }
            group.addView(chip)
        }
    }

    private fun optionChips(group: ChipGroup, options: List<String>, s: RoundState) {
        group.removeAllViews()
        for (o in options) {
            val chip = Chip(requireContext())
            chip.text = o
            chip.textSize = 18f
            chip.isCheckable = false
            val isAnswer = o == s.item.sentence.focusWord
            when {
                s.checked && isAnswer -> chip.setChipBackgroundColorResource(R.color.success_container)
                s.checked && o == s.chosen -> chip.setChipBackgroundColorResource(R.color.error_container)
                o == s.chosen -> chip.setChipBackgroundColorResource(R.color.brand_primary_container)
            }
            chip.setOnClickListener { viewModel.choose(o) }
            group.addView(chip)
        }
    }

    private fun instruction(type: SentenceExercise) = when (type) {
        SentenceExercise.BUILD -> R.string.ex_build
        SentenceExercise.FILL -> R.string.ex_fill
        SentenceExercise.FIX -> R.string.ex_fix
        SentenceExercise.DICTATION -> R.string.ex_dictation
        SentenceExercise.TRANSLATE -> R.string.ex_translate
    }

    private fun instructionGu(type: SentenceExercise) = when (type) {
        SentenceExercise.BUILD -> R.string.ex_build_gu
        SentenceExercise.FILL -> R.string.ex_fill_gu
        SentenceExercise.FIX -> R.string.ex_fix_gu
        SentenceExercise.DICTATION -> R.string.ex_dictation_gu
        SentenceExercise.TRANSLATE -> R.string.ex_translate_gu
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
