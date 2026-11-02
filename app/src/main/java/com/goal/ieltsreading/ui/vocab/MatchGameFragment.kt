package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentMatchGameBinding
import com.goal.ieltsreading.ui.common.G4Ui
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
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
        binding.btnModeSpelling.setOnClickListener {
            findNavController().navigate(
                R.id.spellingFragment, null,
                NavOptions.Builder().setPopUpTo(R.id.matchGameFragment, true).build()
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textEmpty.visibleIf(s.empty)
            binding.columns.visibleIf(!s.empty)
            fillColumn(binding.leftColumn, s.left, s)
            fillColumn(binding.rightColumn, s.right, s)
            binding.rowStatus.visibleIf(!s.empty)
            binding.progressMatch.max = s.left.size.coerceAtLeast(1)
            binding.progressMatch.setProgressCompat(s.matchedIds.size, true)
            binding.textStatus.text = getString(R.string.g4_pairs_matched_format, s.matchedIds.size, s.left.size)
            binding.textMistakes.text = if (s.mistakes == 0) getString(R.string.g4_no_mistakes)
            else resources.getQuantityString(R.plurals.g4_mistakes, s.mistakes, s.mistakes)
            binding.textMistakes.setTextColor(
                ContextCompat.getColor(requireContext(), if (s.mistakes == 0) R.color.g4_green else R.color.g4_red)
            )

            val last = s.lastMatched
            binding.cardLast.visibleIf(last != null)
            if (last != null) {
                val head = if (last.pronunciationGu.isNotBlank()) "${last.word} · ${last.pronunciationGu}" else last.word
                binding.textLast.text = getString(R.string.match_last_format, head, last.meaningGu)
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

    /** Tiles: open = raised white, selected = blue, matched = faded green, wrong pair = red. */
    private fun fillColumn(column: LinearLayout, cards: List<MatchCard>, s: MatchState) {
        column.removeAllViews()
        val ctx = requireContext()
        val gap = resources.getDimensionPixelSize(R.dimen.g4_grid_gap)
        for (card in cards) {
            val done = card.wordId in s.matchedIds
            val isWrong = s.wrong?.let { it.first == card || it.second == card } == true
            val selected = s.selected == card
            val bg = when {
                done -> G4Ui.rounded(ctx, R.color.g4_green_soft, 16f)
                isWrong -> G4Ui.rounded(ctx, R.color.g4_red_soft, 16f, R.color.g4_red, 2.5f)
                selected -> G4Ui.rounded(ctx, R.color.brand_primary_container, 16f, R.color.brand_primary, 2.5f)
                else -> G4Ui.raised(ctx, radiusDp = 16f)
            }
            val tile = G4Ui.tile(
                ctx, card.text, bg,
                textColor = when {
                    done -> R.color.g4_green_text
                    isWrong -> R.color.g4_red_text
                    selected -> R.color.brand_on_primary_container
                    else -> R.color.text_primary
                },
                sizeSp = 17f,
                font = when {
                    selected || isWrong -> R.font.inter_extrabold
                    done -> R.font.inter_bold
                    else -> R.font.inter_semibold
                },
                raisedEdge = !done && !isWrong && !selected,
                padHDp = 8f, padVDp = 0f
            )
            tile.alpha = if (done) 0.55f else 1f
            tile.isEnabled = !done
            tile.setOnClickListener {
                if (card.english) speak(card.text)
                viewModel.tap(card)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, G4Ui.dp(ctx, 58f))
            lp.topMargin = gap
            column.addView(tile, lp)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
