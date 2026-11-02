package com.goal.ieltsreading.ui.vocab

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
import com.goal.ieltsreading.databinding.FragmentVocabHubBinding
import com.goal.ieltsreading.databinding.ItemPackBinding
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.snackbar.Snackbar

/** Vocabulary builder: words-you-know counter, daily new words, review, games and topic packs. */
class VocabHubFragment : Fragment() {

    private var _binding: FragmentVocabHubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VocabHubViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVocabHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cardReview.setOnClickListener { findNavController().navigate(R.id.flashcardFragment) }
        binding.cardMatch.setOnClickListener { findNavController().navigate(R.id.matchGameFragment) }
        binding.btnAllWords.setOnClickListener { findNavController().navigate(R.id.vocabFragment) }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.textKnown.text = s.wordsInDeck.toString()
            binding.textMastered.text = getString(R.string.g4_words_mastered_format, s.mastered)
            binding.progressKnown.setProgressCompat((s.wordsInDeck * 100 / GOAL_WORDS).coerceAtMost(100), true)

            val pack = s.currentPack
            binding.textNewCount.text = s.newToday.toString()
            binding.textCurrentPack.text = pack?.pack?.title ?: getString(R.string.no_pack)
            binding.cardNew.isEnabled = pack != null && s.newToday > 0
            binding.cardNew.alpha = if (binding.cardNew.isEnabled) 1f else 0.6f
            binding.cardNew.setOnClickListener {
                if (pack != null) findNavController().navigate(R.id.wordLearnFragment, bundleOf("packId" to pack.pack.id))
            }
            binding.textDueCount.text = s.due.toString()

            binding.packsGrid.removeAllViews()
            val gap = resources.getDimensionPixelSize(R.dimen.g4_pack_gap)
            s.packs.chunked(2).forEach { pair ->
                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                val rp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rp.topMargin = gap
                binding.packsGrid.addView(row, rp)
                pair.forEachIndexed { i, p ->
                    val item = ItemPackBinding.inflate(layoutInflater, row, false)
                    item.textTitle.text = p.pack.title
                    item.textTitleGu.text = getString(R.string.g4_pack_sub_format, p.pack.titleGu, p.learned, p.total)
                    item.textIelts.visibleIf(p.pack.ielts)
                    item.progress.setIndicatorColor(
                        ContextCompat.getColor(requireContext(), if (p.percent >= 80) R.color.g4_green else R.color.brand_primary)
                    )
                    item.progress.setProgressCompat(p.percent, false)
                    item.iconLock.visibleIf(!p.unlocked)
                    item.root.alpha = if (p.unlocked) 1f else 0.55f
                    item.root.isChecked = p.pack.id == pack?.pack?.id
                    item.root.setOnClickListener {
                        if (p.unlocked) viewModel.choosePack(p.pack.id)
                        else Snackbar.make(binding.root, getString(R.string.pack_locked_format, p.pack.minLevel), Snackbar.LENGTH_LONG).show()
                    }
                    (item.root.layoutParams as LinearLayout.LayoutParams).marginStart = if (i == 0) 0 else gap
                    row.addView(item.root)
                }
                if (pair.size == 1) {
                    val fp = LinearLayout.LayoutParams(0, 0, 1f)
                    fp.marginStart = gap
                    row.addView(View(requireContext()), fp)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Roughly the vocabulary size needed for IELTS Band 7 reading. */
        const val GOAL_WORDS = 4000
    }
}
