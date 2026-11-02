package com.goal.ieltsreading.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentLessonWordsBinding
import com.goal.ieltsreading.databinding.ItemWordCardBinding
import com.google.android.material.snackbar.Snackbar

/** Step 2: the key words of the passage with Gujarati meanings. */
class LessonWordsFragment : Fragment() {

    private var _binding: FragmentLessonWordsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnAddAll.setOnClickListener { viewModel.addAllWordsToDeck(getString(R.string.words_added)) }

        viewModel.words.observe(viewLifecycleOwner) { words ->
            binding.wordsContainer.removeAllViews()
            binding.textCount.text = getString(R.string.key_words_format, words.size)
            for (w in words) {
                val item = ItemWordCardBinding.inflate(layoutInflater, binding.wordsContainer, false)
                item.textWord.text = w.word
                item.textPos.text = w.pos
                item.textMeaningGu.text = w.meaningGu
                item.textMeaningEn.text = w.meaningEn
                item.textExample.text = w.example
                item.textExampleGu.text = w.exampleGu
                binding.wordsContainer.addView(item.root)
            }
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
