package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentVocabBinding
import com.goal.ieltsreading.ui.common.visibleIf

/** Vocabulary hub: review due words (spaced repetition) and browse all words. */
class VocabFragment : Fragment() {

    private var _binding: FragmentVocabBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VocabViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVocabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = WordsAdapter { w ->
            WordSheet.newInstance(w.id, w.word, w.pos, w.meaningGu, w.meaningEn, w.example, w.exampleGu)
                .show(childFragmentManager, "word")
        }
        binding.recycler.adapter = adapter
        binding.inputSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.chipMyDeck.setOnCheckedChangeListener { _, checked -> viewModel.showOnlyDeck(checked) }
        binding.btnReview.setOnClickListener { findNavController().navigate(R.id.flashcardFragment) }

        viewModel.summary.observe(viewLifecycleOwner) { s ->
            binding.textDue.text = s.due.toString()
            binding.textDeck.text = s.deck.toString()
            binding.textMastered.text = s.mastered.toString()
            binding.btnReview.isEnabled = s.due > 0
            binding.btnReview.text = if (s.due > 0) getString(R.string.btn_review_now, s.due) else getString(R.string.nothing_due)
        }
        viewModel.words.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.textEmpty.visibleIf(list.isEmpty())
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
}
