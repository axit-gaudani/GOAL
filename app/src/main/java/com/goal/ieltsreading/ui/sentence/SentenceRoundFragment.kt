package com.goal.ieltsreading.ui.sentence

import android.graphics.Paint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.SentenceExercise
import com.goal.ieltsreading.databinding.FragmentSentenceRoundBinding
import com.goal.ieltsreading.databinding.ItemMissedSentenceBinding
import com.goal.ieltsreading.domain.SentenceChecker
import com.goal.ieltsreading.ui.common.G4Ui
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.chip.ChipGroup
import java.util.Locale

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
            binding.textResultScore.text = getString(R.string.g4_score_format, r.correct, r.total)
            binding.textResultEmpty.visibleIf(r.total == 0)
            binding.rowStats.visibleIf(r.total > 0)
            binding.textResultTime.text = getString(R.string.g4_time_format, r.seconds / 60, r.seconds % 60)
            binding.textResultAccuracy.text =
                getString(R.string.percent_format, if (r.total == 0) 0 else r.correct * 100 / r.total)
            binding.textResultStreak.text = resources.getQuantityString(R.plurals.g4_days, r.streak, r.streak)
            binding.rowMissedHeader.visibleIf(r.missed.isNotEmpty())
            binding.textMissedHeader.visibleIf(r.missed.isNotEmpty())
            binding.textMissedCount.text = resources.getQuantityString(R.plurals.g4_sentences, r.missed.size, r.missed.size)
            binding.missedContainer.removeAllViews()
            for (m in r.missed) {
                val item = ItemMissedSentenceBinding.inflate(layoutInflater, binding.missedContainer, false)
                item.textCorrect.text = withFocus(m.en, m.focus)
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
        binding.textType.text = getString(R.string.exercise_label_format, type.title.uppercase(Locale.ROOT), sentence.pattern)
        binding.textRepeat.visibleIf(item.isRepeat)
        binding.textInstruction.text = getString(instruction(type))
        binding.textInstructionGu.text = getString(instructionGu(type))

        // Prompt
        val showGujarati = type == SentenceExercise.BUILD || type == SentenceExercise.TRANSLATE
        binding.cardPromptGu.visibleIf(showGujarati)
        binding.textPromptGu.text = sentence.gu
        binding.groupListen.visibleIf(type == SentenceExercise.DICTATION)
        binding.cardWords.visibleIf(type == SentenceExercise.FILL || type == SentenceExercise.FIX)
        if (type == SentenceExercise.FILL) {
            sentenceWords(binding.wordsGroup, viewModel.fillWords(item, s.chosen ?: "____"),
                SentenceChecker.focusIndex(sentence), s, onTap = null)
        } else if (type == SentenceExercise.FIX) {
            sentenceWords(binding.wordsGroup, viewModel.fixWords(item), s.tappedWrong, s) { viewModel.tapWord(it) }
        }

        // Answer area
        binding.groupBuild.visibleIf(type == SentenceExercise.BUILD)
        if (type == SentenceExercise.BUILD) {
            buildTiles(binding.answerGroup, s.answer.map { it.text }, placed = true) { i -> viewModel.tapAnswer(s.answer[i]) }
            buildTiles(binding.bankGroup, s.bank.map { it.text }, placed = false) { i -> viewModel.tapBank(s.bank[i]) }
        }
        val needsOptions = type == SentenceExercise.FILL || (type == SentenceExercise.FIX && s.tappedWrong >= 0)
        binding.textOptionsHeader.visibleIf(needsOptions)
        binding.optionsGroup.visibleIf(needsOptions)
        if (needsOptions) {
            optionTiles(binding.optionsGroup, sentence.focusOptions.filter { it != sentence.focusWrong || type == SentenceExercise.FILL }, s)
        }
        val typing = type == SentenceExercise.DICTATION || type == SentenceExercise.TRANSLATE
        binding.inputLayout.visibleIf(typing)
        binding.textInputLabel.visibleIf(typing)
        binding.textInputLabel.setText(
            if (type == SentenceExercise.TRANSLATE) R.string.g4_label_your_english else R.string.g4_label_what_you_heard
        )
        if (newSentence) {
            binding.inputAnswer.setText("")
            binding.scroll.scrollTo(0, 0)
            if (type == SentenceExercise.DICTATION) speak(sentence.en)
        }
        binding.inputAnswer.isEnabled = !s.checked

        // Feedback: the bottom panel turns green or red and shows the right sentence
        val ctx = requireContext()
        binding.cardFeedback.visibleIf(s.checked)
        if (s.checked) {
            val ok = s.correct
            binding.bottomPanel.setBackgroundColor(G4Ui.color(ctx, if (ok) R.color.g4_green_soft else R.color.g4_red_soft))
            binding.iconFeedback.setBackgroundResource(if (ok) R.drawable.g4_bg_circle_green else R.drawable.g4_bg_circle_red)
            binding.iconFeedback.setImageResource(if (ok) R.drawable.ic_g4_check else R.drawable.ic_g4_close)
            binding.textFeedbackTitle.setText(if (ok) R.string.g4_feedback_correct else R.string.g4_feedback_wrong)
            val textColor = G4Ui.color(ctx, if (ok) R.color.g4_green_text else R.color.g4_red_text)
            binding.textFeedbackTitle.setTextColor(textColor)
            binding.textTip.setTextColor(textColor)
            binding.btnAction.backgroundTintList = ContextCompat.getColorStateList(ctx, if (ok) R.color.g4_green else R.color.g4_red)
            binding.textCorrectSentence.text = sentence.en
            binding.textCorrectGu.text = sentence.gu
            binding.textTip.text = sentence.tipGu.ifBlank { sentence.tip }
            binding.textTip.visibleIf(binding.textTip.text.isNotBlank())
            if (spokenIndex != s.index) {
                spokenIndex = s.index
                speak(sentence.en)
            }
        } else {
            binding.bottomPanel.background = null
            binding.btnAction.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.g4_btn_primary)
        }
        binding.btnAction.setText(
            when {
                !s.checked -> R.string.g4_btn_check
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

    /**
     * The sentence as large words (FILL, FIX). [marked] is the word to highlight: for FIX the tapped
     * word turns amber (crossed out once a fix is chosen, with the fix in green after it); for FILL the gap is blue.
     * [onTap] makes the words tappable (FIX).
     */
    private fun sentenceWords(group: ChipGroup, words: List<String>, marked: Int, s: RoundState, onTap: ((Int) -> Unit)?) {
        group.removeAllViews()
        val ctx = requireContext()
        val isFix = onTap != null
        words.forEachIndexed { i, w ->
            val isMarked = i == marked
            val bg = when {
                isMarked && isFix -> G4Ui.rounded(ctx, R.color.g4_amber_soft, 10f)
                isMarked -> G4Ui.rounded(ctx, R.color.brand_primary_container, 10f)
                else -> G4Ui.rounded(ctx, android.R.color.transparent, 10f)
            }
            val tile = G4Ui.tile(
                ctx, w, bg,
                textColor = when {
                    isMarked && isFix -> R.color.g4_amber_text
                    isMarked -> R.color.brand_on_primary_container
                    else -> R.color.text_primary
                },
                sizeSp = 22f,
                font = if (isMarked) R.font.inter_extrabold else R.font.inter_semibold,
                padHDp = if (isMarked) 10f else 4f,
                padVDp = 6f
            )
            tile.minHeight = G4Ui.dp(ctx, 44f)
            tile.minimumHeight = G4Ui.dp(ctx, 44f)
            tile.isClickable = isFix
            if (isMarked && isFix && s.chosen != null) {
                tile.paintFlags = tile.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            if (onTap != null) tile.setOnClickListener { onTap(i) } else tile.isFocusable = false
            group.addView(tile)
            // FIX: show the chosen correction right after the crossed-out word
            if (isMarked && isFix && s.chosen != null) {
                val fix = G4Ui.tile(
                    ctx, s.chosen, G4Ui.rounded(ctx, R.color.g4_green_soft, 10f),
                    textColor = R.color.g4_green_text, sizeSp = 22f, font = R.font.inter_extrabold,
                    padHDp = 10f, padVDp = 6f
                )
                fix.minHeight = G4Ui.dp(ctx, 44f)
                fix.minimumHeight = G4Ui.dp(ctx, 44f)
                fix.isClickable = false
                fix.isFocusable = false
                group.addView(fix)
            }
        }
    }

    /** BUILD word tiles: raised white tiles in the bank, blue tiles once placed in the answer. */
    private fun buildTiles(group: ChipGroup, words: List<String>, placed: Boolean, onTap: (Int) -> Unit) {
        group.removeAllViews()
        val ctx = requireContext()
        words.forEachIndexed { i, w ->
            val tile = if (placed) {
                G4Ui.tile(ctx, w, G4Ui.rounded(ctx, R.color.brand_primary, 14f), R.color.g4_on_color)
            } else {
                G4Ui.tile(ctx, w, G4Ui.raised(ctx), R.color.text_primary, raisedEdge = true)
            }
            tile.setOnClickListener { onTap(i) }
            group.addView(tile)
        }
    }

    /** Answer choices (FILL, FIX): 3 per row, chosen = blue, after checking right = green and wrong = red. */
    private fun optionTiles(group: LinearLayout, options: List<String>, s: RoundState) {
        group.removeAllViews()
        val ctx = requireContext()
        val gap = resources.getDimensionPixelSize(R.dimen.g4_grid_gap)
        options.chunked(3).forEach { rowOptions ->
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            val rp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rp.topMargin = gap
            group.addView(row, rp)
            for (col in 0 until 3) {
                val o = rowOptions.getOrNull(col)
                val lp = LinearLayout.LayoutParams(0, G4Ui.dp(ctx, 54f), 1f)
                if (col > 0) lp.marginStart = gap
                if (o == null) {
                    row.addView(View(ctx), lp)
                    continue
                }
                val isAnswer = o == s.item.sentence.focusWord
                val good = s.checked && isAnswer
                val bad = s.checked && !isAnswer && o == s.chosen
                val picked = !s.checked && o == s.chosen
                val bg = when {
                    good -> G4Ui.rounded(ctx, R.color.g4_green_soft, 14f, R.color.g4_green, 2.5f)
                    bad -> G4Ui.rounded(ctx, R.color.g4_red_soft, 14f, R.color.g4_red, 2.5f)
                    picked -> G4Ui.rounded(ctx, R.color.brand_primary_container, 14f, R.color.brand_primary, 2.5f)
                    else -> G4Ui.rounded(ctx, R.color.surface, 14f, R.color.track, 1.5f)
                }
                val tile = G4Ui.tile(
                    ctx, o, bg,
                    textColor = when {
                        good -> R.color.g4_green_text
                        bad -> R.color.g4_red_text
                        picked -> R.color.brand_on_primary_container
                        else -> R.color.text_secondary
                    },
                    sizeSp = 17f,
                    font = if (good || bad || picked) R.font.inter_extrabold else R.font.inter_semibold,
                    padHDp = 6f, padVDp = 0f
                )
                tile.setOnClickListener { viewModel.choose(o) }
                row.addView(tile, lp)
            }
        }
    }

    /** The sentence with its focus word in green (result list). */
    private fun withFocus(en: String, focus: String): CharSequence {
        if (focus.isBlank()) return en
        val match = Regex("\\b" + Regex.escape(focus) + "\\b").find(en) ?: return en
        val span = SpannableString(en)
        span.setSpan(
            ForegroundColorSpan(G4Ui.color(requireContext(), R.color.g4_green)),
            match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
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
