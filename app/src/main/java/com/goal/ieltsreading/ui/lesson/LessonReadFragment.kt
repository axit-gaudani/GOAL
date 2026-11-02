package com.goal.ieltsreading.ui.lesson

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.goal.ieltsreading.data.db.LessonEntity
import com.goal.ieltsreading.data.db.VocabEntity
import com.goal.ieltsreading.databinding.FragmentLessonReadBinding
import com.goal.ieltsreading.databinding.ItemParagraphBinding
import com.goal.ieltsreading.ui.common.WordHighlighter
import com.goal.ieltsreading.ui.common.paragraphs
import com.goal.ieltsreading.ui.common.visibleIf
import com.goal.ieltsreading.ui.vocab.WordSheet
import com.google.android.material.color.MaterialColors

/** Step 3: read the passage. Key words are tappable; Gujarati can be shown under each paragraph. */
class LessonReadFragment : Fragment() {

    private var _binding: FragmentLessonReadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonReadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Reading English first is better practice, so translation starts hidden here.
        binding.switchTranslation.isChecked = false
        binding.switchTranslation.setOnCheckedChangeListener { _, _ -> render() }
        viewModel.lesson.observe(viewLifecycleOwner) { render() }
        viewModel.words.observe(viewLifecycleOwner) { render() }
    }

    private fun render() {
        val lesson: LessonEntity = viewModel.lesson.value ?: return
        val words: List<VocabEntity> = viewModel.words.value.orEmpty()
        val showGu = binding.switchTranslation.isChecked
        val color = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)

        binding.textPassageTitle.text = lesson.passageTitle
        val wordCount = lesson.passage.split(Regex("\\s+")).count { it.isNotBlank() }
        binding.textWordCount.text = getString(com.goal.ieltsreading.R.string.word_count_format, wordCount)

        val en = lesson.passage.paragraphs()
        val gu = lesson.passageGu.paragraphs()
        binding.paragraphContainer.removeAllViews()
        en.forEachIndexed { index, paragraph ->
            val item = ItemParagraphBinding.inflate(layoutInflater, binding.paragraphContainer, false)
            item.textEnglish.text = WordHighlighter.highlight(paragraph, words, color) { word ->
                WordSheet.newInstance(word.id, word.word, word.pos, word.meaningGu, word.meaningEn, word.example, word.exampleGu)
                    .show(childFragmentManager, "word")
            }
            item.textEnglish.movementMethod = LinkMovementMethod.getInstance()
            // If paragraph counts match, show each Gujarati paragraph under its English one.
            val guText = if (gu.size == en.size) gu[index] else if (index == en.lastIndex) gu.joinToString("\n\n") else ""
            item.textGujarati.text = guText
            item.textGujarati.visibleIf(showGu && guText.isNotBlank())
            binding.paragraphContainer.addView(item.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
