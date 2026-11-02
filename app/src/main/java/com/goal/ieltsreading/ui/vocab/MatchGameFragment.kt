package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentMatchGameBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.button.MaterialButton
import android.content.res.ColorStateList
import android.widget.LinearLayout

class MatchGameFragment : Fragment() {

    private var _binding: FragmentMatchGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MatchGameViewModel by viewModels()
    private var spokenId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMatchGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        binding.btnAgain.setOnClickListener { viewModel.newGame() }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textEmpty.visibleIf(s.empty)
            binding.columns.visibleIf(!s.empty)
            fillColumn(binding.leftColumn, s.left, s)
            fillColumn(binding.rightColumn, s.right, s)
            binding.textStatus.text = getString(R.string.match_status, s.matchedIds.size, s.left.size, s.mistakes)

            val last = s.lastMatched
            binding.cardLast.visibleIf(last != null)
            if (last != null) {
                binding.textLast.text = getString(R.string.match_last_format, last.word, last.meaningGu)
                binding.textLastExample.text = last.example
                binding.btnPlayLast.setOnClickListener { speak(last.word) }
                if (spokenId != last.id) {
                    spokenId = last.id
                    speak(last.word)
                }
            }
            binding.groupFinished.visibleIf(s.finished)
            if (s.finished) binding.textFinished.text = getString(R.string.match_finished, s.seconds, s.mistakes)
        }
    }

    private fun fillColumn(column: LinearLayout, cards: List<MatchCard>, s: MatchState) {
        column.removeAllViews()
        val ctx = requireContext()
        for (card in cards) {
            val button = layoutInflater.inflate(R.layout.view_option_button, column, false) as MaterialButton
            button.text = card.text
            button.textAlignment = View.TEXT_ALIGNMENT_CENTER
            button.gravity = android.view.Gravity.CENTER
            val done = card.wordId in s.matchedIds
            val isWrong = s.wrong?.let { it.first == card || it.second == card } == true
            val colorRes = when {
                done -> R.color.success
                isWrong -> R.color.error
                s.selected == card -> R.color.brand_primary
                else -> R.color.option_stroke
            }
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, colorRes))
            button.strokeWidth = resources.getDimensionPixelSize(
                if (done || isWrong || s.selected == card) R.dimen.stroke_selected else R.dimen.stroke_normal
            )
            button.alpha = if (done) 0.5f else 1f
            button.isEnabled = !done
            button.setOnClickListener {
                if (card.english) speak(card.text)
                viewModel.tap(card)
            }
            column.addView(button)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
