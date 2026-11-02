package com.goal.ieltsreading.ui.lesson

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.content.AbcLetter
import com.goal.ieltsreading.databinding.FragmentLessonLettersBinding
import com.goal.ieltsreading.databinding.ItemAbcWordBinding
import com.goal.ieltsreading.ui.common.G4Ui
import com.goal.ieltsreading.ui.common.speak
import com.google.android.material.button.MaterialButton

/**
 * ABC step: a big card for the chosen letter (name and sound in Gujarati, example words)
 * and a grid of all letters in this lesson. Every letter and word can be heard.
 */
class LessonLettersFragment : Fragment() {

    private var _binding: FragmentLessonLettersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels({ requireParentFragment() })
    private var selected = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonLettersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getInt(KEY_SELECTED) ?: 0
        binding.btnListen.setOnClickListener { current()?.let { speakLetter(it, slow = false) } }
        binding.btnSlow.setOnClickListener { current()?.let { speakLetter(it, slow = true) } }
        binding.btnNextLetter.setOnClickListener {
            if (selected < viewModel.letters.lastIndex) select(selected + 1)
        }
        render()
    }

    private fun current(): AbcLetter? = viewModel.letters.getOrNull(selected)

    /** Says the letter, then its first example word: "B. ball." */
    private fun speakLetter(letter: AbcLetter, slow: Boolean) {
        val word = letter.words.firstOrNull()?.word
        speak(if (word != null) "${letter.upper}. $word." else "${letter.upper}.", slow)
    }

    private fun select(index: Int) {
        selected = index
        render()
        current()?.let { speakLetter(it, slow = false) }
    }

    private fun render() {
        val letters = viewModel.letters
        val letter = current() ?: return
        binding.textBigLetter.text = getString(R.string.g4_letter_big_format, letter.upper, letter.lower)
        binding.textName.text = letter.nameGu
        binding.textSound.text = letter.soundGu
        binding.textPosition.text = getString(R.string.counter_format, selected + 1, letters.size)
        binding.btnNextLetter.isEnabled = selected < letters.lastIndex

        binding.wordsContainer.removeAllViews()
        for (w in letter.words) {
            val item = ItemAbcWordBinding.inflate(layoutInflater, binding.wordsContainer, false)
            item.textWord.text = w.word
            item.textPron.text = w.pronGu
            item.textGu.text = w.gu
            item.root.setOnClickListener { speak(w.word) }
            binding.wordsContainer.addView(item.root)
        }

        binding.lettersGrid.removeAllViews()
        val ctx = requireContext()
        letters.forEachIndexed { i, l ->
            val button = layoutInflater.inflate(R.layout.view_letter_tile, binding.lettersGrid, false) as MaterialButton
            button.text = tileText(l)
            button.contentDescription = getString(R.string.letter_pair_format, l.upper, l.lower)
            // Letters before the current one are done (soft green), the current one has a blue border.
            val (bg, fg, stroke) = when {
                i < selected -> Triple(R.color.g4_green_soft, R.color.g4_green_text, null)
                i == selected -> Triple(R.color.surface, R.color.brand_primary, R.color.brand_primary)
                else -> Triple(R.color.surface, R.color.text_secondary, R.color.track)
            }
            button.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)
            button.setTextColor(ContextCompat.getColor(ctx, fg))
            if (stroke != null) button.strokeColor = ContextCompat.getColorStateList(ctx, stroke)
            button.strokeWidth = when {
                stroke == null -> 0
                i == selected -> G4Ui.dp(ctx, 2.5f)
                else -> G4Ui.dp(ctx, 1.5f)
            }
            button.setOnClickListener { select(i) }
            binding.lettersGrid.addView(button)
        }
    }

    /** Big capital letter with its Gujarati name in small text under it. */
    private fun tileText(l: AbcLetter): CharSequence {
        if (l.nameGu.isBlank()) return l.upper
        val sb = SpannableStringBuilder(l.upper).append("\n")
        val start = sb.length
        sb.append(l.nameGu)
        sb.setSpan(AbsoluteSizeSpan(12, true), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(FontSpan(ResourcesCompat.getFont(requireContext(), R.font.inter_semibold)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED, selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Draws part of a text in another font (TypefaceSpan(Typeface) needs API 28). */
    private class FontSpan(private val typeface: Typeface?) : MetricAffectingSpan() {
        override fun updateDrawState(paint: TextPaint) { typeface?.let { paint.typeface = it } }
        override fun updateMeasureState(paint: TextPaint) { typeface?.let { paint.typeface = it } }
    }

    private companion object {
        const val KEY_SELECTED = "selected"
    }
}
