package com.goal.ieltsreading.ui.vocab

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentWordLearnBinding
import com.goal.ieltsreading.databinding.ItemWordFormBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf

/** A full word card: sound, meaning, example, word family, synonyms, opposites, collocations. */
class WordLearnFragment : Fragment() {

    private companion object {
        /** Word-family tile colours: card background to text colour. */
        val FORM_COLORS = listOf(
            R.color.brand_primary_container to R.color.brand_on_primary_container,
            R.color.g4_amber_soft to R.color.g4_amber_text,
            R.color.g4_teal_soft to R.color.g4_teal_text
        )
    }

    private var _binding: FragmentWordLearnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WordLearnViewModel by viewModels()
    private var spokenId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWordLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        binding.btnKnow.setOnClickListener { viewModel.answer(alreadyKnown = true) }
        binding.btnLearn.setOnClickListener { viewModel.answer(alreadyKnown = false) }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.groupCard.visibleIf(!s.finished)
            binding.groupFinished.visibleIf(s.finished)
            if (s.finished) {
                binding.textFinished.text = getString(R.string.words_learned_today, s.total)
                return@observe
            }
            val w = s.word ?: return@observe
            binding.textCounter.text = if (s.packTitle.isNotBlank())
                getString(R.string.g4_pack_word_counter, s.packTitle, s.position, s.total)
            else getString(R.string.new_word_counter, s.position, s.total)
            binding.progress.max = s.total
            binding.progress.setProgressCompat(s.position - 1, true)
            binding.textWord.text = w.word
            binding.textPos.text = listOf(w.pos, w.pronunciationGu).filter { it.isNotBlank() }.joinToString(" · ")
            binding.textMeaningGu.text = w.meaningGu
            binding.textMeaningEn.text = w.meaningEn
            binding.textExample.text = boldWord(w.example, w.word)
            binding.textExampleGu.text = w.exampleGu
            binding.btnPlay.setOnClickListener { speak(w.word) }
            binding.btnPlayExample.setOnClickListener { speak(w.example) }
            binding.btnPlayExample.visibleIf(w.example.isNotBlank())

            binding.familyContainer.removeAllViews()
            val forms = w.family.filter { it.word.isNotBlank() }
            binding.textFamilyHeader.visibleIf(forms.size > 1)
            binding.familyContainer.visibleIf(forms.size > 1)
            val gap = resources.getDimensionPixelSize(R.dimen.g4_pack_gap)
            forms.chunked(3).forEachIndexed { rowIndex, rowForms ->
                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                val rp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rp.topMargin = gap
                binding.familyContainer.addView(row, rp)
                for (col in 0 until 3) {
                    val f = rowForms.getOrNull(col)
                    if (f == null) {
                        val fp = LinearLayout.LayoutParams(0, 0, 1f)
                        fp.marginStart = gap
                        row.addView(View(requireContext()), fp)
                        continue
                    }
                    val item = ItemWordFormBinding.inflate(layoutInflater, row, false)
                    // Blue, amber, teal like the design
                    val (bg, fg) = FORM_COLORS[(rowIndex * 3 + col) % FORM_COLORS.size]
                    val fgColor = ContextCompat.getColor(requireContext(), fg)
                    item.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bg))
                    item.textPos.setTextColor(fgColor)
                    item.textForm.setTextColor(fgColor)
                    if (fg != R.color.brand_on_primary_container) item.textGu.setTextColor(fgColor)
                    item.textPos.text = f.pos.uppercase()
                    item.textForm.text = f.word
                    item.textGu.text = f.gu
                    item.root.setOnClickListener { speak(f.word) }
                    (item.root.layoutParams as LinearLayout.LayoutParams).marginStart = if (col == 0) 0 else gap
                    row.addView(item.root)
                }
            }

            binding.rowSynonyms.visibleIf(w.synonyms.isNotEmpty())
            binding.textSynonyms.text = w.synonyms.joinToString(", ")
            binding.rowOpposites.visibleIf(w.opposites.isNotEmpty())
            binding.textOpposites.text = w.opposites.joinToString(", ")
            binding.rowCollocations.visibleIf(w.collocations.isNotEmpty())
            binding.textCollocations.text = w.collocations.joinToString(", ")
            binding.textIeltsTip.text = w.ieltsTip
            binding.textIeltsTip.visibleIf(w.ieltsTip.isNotBlank())

            if (spokenId != w.id && viewModel.autoSpeak) {
                spokenId = w.id
                speak(w.word)
            }
        }
    }

    /** The example sentence with the new word in bold. */
    private fun boldWord(example: String, word: String): CharSequence {
        val match = Regex("\\b" + Regex.escape(word) + "\\w*", RegexOption.IGNORE_CASE).find(example) ?: return example
        val span = SpannableString(example)
        span.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
