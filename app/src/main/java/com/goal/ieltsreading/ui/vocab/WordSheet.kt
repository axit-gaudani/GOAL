package com.goal.ieltsreading.ui.vocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.SheetWordBinding
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/** Bottom sheet that shows one word's meaning, opened by tapping a word. */
class WordSheet : BottomSheetDialogFragment() {

    private var _binding: SheetWordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetWordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        binding.textWord.text = args.getString(ARG_WORD)
        binding.textPos.text = args.getString(ARG_POS)
        binding.textMeaningGu.text = args.getString(ARG_GU)
        binding.textMeaningEn.text = args.getString(ARG_EN)
        val example = args.getString(ARG_EXAMPLE).orEmpty()
        binding.textExample.text = example
        binding.textExampleGu.text = args.getString(ARG_EXAMPLE_GU)
        binding.groupExample.visibleIf(example.isNotBlank())
        val pronunciation = args.getString(ARG_PRONUNCIATION_GU).orEmpty()
        binding.textPronunciation.text = pronunciation
        binding.textPronunciation.visibleIf(pronunciation.isNotBlank())
        binding.textPos.visibleIf(!args.getString(ARG_POS).isNullOrBlank())

        val word = args.getString(ARG_WORD).orEmpty()
        binding.btnPlay.setOnClickListener { app.speaker.speak(word) }
        if (app.prefs.autoSpeak) app.speaker.speak(word)

        binding.btnAddToDeck.setOnClickListener {
            val id = args.getString(ARG_ID) ?: return@setOnClickListener
            val appRef = app
            appRef.appScope.launch { appRef.repository.addWordToDeck(id) }
            Toast.makeText(requireContext(), R.string.word_added, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_WORD = "word"
        private const val ARG_POS = "pos"
        private const val ARG_GU = "gu"
        private const val ARG_EN = "en"
        private const val ARG_EXAMPLE = "example"
        private const val ARG_EXAMPLE_GU = "exampleGu"
        private const val ARG_PRONUNCIATION_GU = "pronunciationGu"

        fun newInstance(
            id: String, word: String, pos: String, meaningGu: String, meaningEn: String, example: String, exampleGu: String,
            pronunciationGu: String = ""
        ) = WordSheet().apply {
            arguments = bundleOf(
                ARG_ID to id, ARG_WORD to word, ARG_POS to pos, ARG_GU to meaningGu, ARG_EN to meaningEn,
                ARG_EXAMPLE to example, ARG_EXAMPLE_GU to exampleGu, ARG_PRONUNCIATION_GU to pronunciationGu
            )
        }
    }
}
