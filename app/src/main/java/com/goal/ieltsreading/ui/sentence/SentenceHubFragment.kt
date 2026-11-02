package com.goal.ieltsreading.ui.sentence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.SentenceExercise
import com.goal.ieltsreading.databinding.FragmentSentenceHubBinding
import com.goal.ieltsreading.databinding.ItemG4PatternBinding
import com.goal.ieltsreading.databinding.ItemModeButtonBinding
import com.goal.ieltsreading.ui.common.visibleIf

/** Sentence practice home: today's round, grammar mastery and single-skill practice. */
class SentenceHubFragment : Fragment() {

    private var _binding: FragmentSentenceHubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SentenceHubViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSentenceHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnStart.setOnClickListener { startRound(null) }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textLevel.text = getString(R.string.sentence_round_level, s.levelName)
            val repeat = minOf(s.due, 4)
            binding.textRepeat.text = getString(R.string.g4_to_repeat_format, repeat)
            binding.textRepeat.visibleIf(s.due > 0)
            // A round takes up to 4 sentences to repeat, the rest are new (see LearningRepository.sentenceRound).
            binding.textRoundMix.text = if (repeat > 0) getString(R.string.g4_round_mix_format, 10 - repeat, repeat)
            else getString(R.string.g4_round_all_new)

            binding.patternsContainer.removeAllViews()
            binding.textNoPatterns.visibleIf(s.patterns.isEmpty())
            for (p in s.patterns.take(5)) {
                val item = ItemG4PatternBinding.inflate(layoutInflater, binding.patternsContainer, false)
                item.textName.text = p.pattern
                item.textPercent.text = getString(R.string.percent_format, p.percent)
                item.progress.setIndicatorColor(
                    ContextCompat.getColor(requireContext(), if (p.percent < 60) R.color.brand_secondary else R.color.brand_primary)
                )
                item.progress.setProgressCompat(p.percent, false)
                binding.patternsContainer.addView(item.root)
            }

            // Skills in a 2-column grid
            binding.modesContainer.removeAllViews()
            val gap = resources.getDimensionPixelSize(R.dimen.g4_grid_gap)
            SentenceExercise.entries.chunked(2).forEach { pair ->
                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rowParams.topMargin = gap
                binding.modesContainer.addView(row, rowParams)
                pair.forEachIndexed { i, mode ->
                    val item = ItemModeButtonBinding.inflate(layoutInflater, row, false)
                    item.textTitle.text = mode.title
                    item.textTitleGu.text = mode.titleGu
                    val open = mode.minLevel <= s.level
                    item.root.isEnabled = open
                    item.root.alpha = if (open) 1f else 0.5f
                    item.textLocked.visibleIf(!open)
                    item.textLocked.text = getString(R.string.opens_at_level, mode.minLevel)
                    item.root.setOnClickListener { startRound(mode) }
                    (item.root.layoutParams as LinearLayout.LayoutParams).marginStart = if (i == 0) 0 else gap
                    row.addView(item.root)
                }
                // Odd count: keep the last tile half width
                if (pair.size == 1) {
                    val filler = android.view.View(requireContext())
                    val fp = LinearLayout.LayoutParams(0, 0, 1f)
                    fp.marginStart = gap
                    row.addView(filler, fp)
                }
            }
        }
    }

    private fun startRound(mode: SentenceExercise?) {
        findNavController().navigate(R.id.sentenceRoundFragment, bundleOf("mode" to (mode?.name ?: "")))
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
