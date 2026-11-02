package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            binding.textCounter.text = getString(R.string.new_word_counter, s.position, s.total)
            binding.progress.max = s.total
            binding.progress.setProgressCompat(s.position - 1, true)
            binding.textWord.text = w.word
            binding.textPos.text = listOf(w.pos, w.pronunciationGu).filter { it.isNotBlank() }.joinToString(" · ")
            binding.textMeaningGu.text = w.meaningGu
            binding.textMeaningEn.text = w.meaningEn
            binding.textExample.text = w.example
            binding.textExampleGu.text = w.exampleGu
            binding.btnPlay.setOnClickListener { speak(w.word) }
            binding.btnPlayExample.setOnClickListener { speak(w.example) }
            binding.btnPlayExample.visibleIf(w.example.isNotBlank())

            binding.familyContainer.removeAllViews()
            val forms = w.family.filter { it.word.isNotBlank() }
            binding.textFamilyHeader.visibleIf(forms.size > 1)
            binding.familyContainer.visibleIf(forms.size > 1)
            for (f in forms) {
                val item = ItemWordFormBinding.inflate(layoutInflater, binding.familyContainer, false)
                item.textPos.text = f.pos.uppercase()
                item.textForm.text = f.word
                item.textGu.text = f.gu
                item.root.setOnClickListener { speak(f.word) }
                binding.familyContainer.addView(item.root)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
