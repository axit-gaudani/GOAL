package com.goal.ieltsreading.ui.sentence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.SentenceExercise
import com.goal.ieltsreading.databinding.FragmentSentenceHubBinding
import com.goal.ieltsreading.databinding.ItemLevelProgressBinding
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
            binding.textRepeat.text = getString(R.string.sentence_repeat_format, minOf(s.due, 4))
            binding.textRepeat.visibleIf(s.due > 0)

            binding.patternsContainer.removeAllViews()
            binding.textNoPatterns.visibleIf(s.patterns.isEmpty())
            for (p in s.patterns.take(5)) {
                val item = ItemLevelProgressBinding.inflate(layoutInflater, binding.patternsContainer, false)
                item.textName.text = p.pattern
                item.textCount.text = getString(R.string.percent_format, p.percent)
                item.progress.setProgressCompat(p.percent, false)
                binding.patternsContainer.addView(item.root)
            }

            binding.modesContainer.removeAllViews()
            for (mode in SentenceExercise.entries) {
                val item = ItemModeButtonBinding.inflate(layoutInflater, binding.modesContainer, false)
                item.textTitle.text = mode.title
                item.textTitleGu.text = mode.titleGu
                val open = mode.minLevel <= s.level
                item.root.isEnabled = open
                item.root.alpha = if (open) 1f else 0.5f
                item.textLocked.visibleIf(!open)
                item.textLocked.text = getString(R.string.opens_at_level, mode.minLevel)
                item.root.setOnClickListener { startRound(mode) }
                binding.modesContainer.addView(item.root)
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
