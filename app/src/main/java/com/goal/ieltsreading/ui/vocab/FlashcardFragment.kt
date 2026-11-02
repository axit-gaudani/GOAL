package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentFlashcardBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf

class FlashcardFragment : Fragment() {

    private var _binding: FragmentFlashcardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FlashcardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        binding.cardWord.setOnClickListener { viewModel.reveal() }
        binding.btnShow.setOnClickListener { viewModel.reveal() }
        binding.btnKnew.setOnClickListener { viewModel.answer(true) }
        binding.btnDidNotKnow.setOnClickListener { viewModel.answer(false) }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }
        // Two-line answer buttons: English on top, smaller Gujarati below (as in the design).
        binding.btnKnew.text = twoLine(getString(R.string.g3_btn_knew), getString(R.string.g3_btn_knew_gu))
        binding.btnDidNotKnow.text = twoLine(getString(R.string.g3_btn_not_knew), getString(R.string.g3_btn_not_knew_gu))

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.groupCard.visibleIf(!s.finished)
            binding.groupFinished.visibleIf(s.finished)
            if (s.finished) {
                binding.textFinishedTitle.text = getString(
                    if (s.total == 0) R.string.review_nothing_title else R.string.review_done_title
                )
                binding.textFinishedStats.text = if (s.total == 0) getString(R.string.review_nothing_text)
                else getString(R.string.review_done_stats, s.known, s.unknown)
                return@observe
            }
            val w = s.word ?: return@observe
            binding.progress.max = s.total
            binding.progress.setProgressCompat(s.position - 1, true)
            binding.textCounter.text = getString(R.string.counter_format, s.position, s.total)
            binding.textWord.text = w.word
            binding.btnPlay.setOnClickListener { speak(w.word) }
            binding.textPos.text = w.pos
            binding.textPos.visibleIf(w.pos.isNotBlank())
            binding.textBox.text = w.box?.let { getString(R.string.g3_box_format, it) }
            binding.textBox.visibleIf(w.box != null)
            binding.textExample.text = w.example
            binding.groupAnswer.visibleIf(s.revealed)
            binding.btnShow.visibleIf(!s.revealed)
            binding.textHint.visibleIf(!s.revealed)
            binding.groupButtons.visibleIf(s.revealed)
            binding.textMeaningGu.text = w.meaningGu
            binding.textMeaningEn.text = w.meaningEn
            binding.textExampleGu.text = w.exampleGu
        }
    }

    private fun twoLine(main: String, sub: String): CharSequence {
        val sb = SpannableStringBuilder(main).append('\n')
        val start = sb.length
        sb.append(sub)
        sb.setSpan(RelativeSizeSpan(13f / 16f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
