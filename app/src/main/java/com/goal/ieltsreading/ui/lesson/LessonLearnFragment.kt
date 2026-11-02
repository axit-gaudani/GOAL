package com.goal.ieltsreading.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.goal.ieltsreading.databinding.FragmentLessonLearnBinding
import com.goal.ieltsreading.databinding.ItemExampleBinding
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf

/** Step 1: the teaching explanation in simple English with Gujarati help. */
class LessonLearnFragment : Fragment() {

    private var _binding: FragmentLessonLearnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.switchGujarati.isChecked = viewModel.showGujaratiByDefault
        binding.cardGujarati.visibleIf(viewModel.showGujaratiByDefault)
        binding.switchGujarati.setOnCheckedChangeListener { _, checked -> binding.cardGujarati.visibleIf(checked) }

        viewModel.lesson.observe(viewLifecycleOwner) { lesson ->
            if (lesson == null) return@observe
            binding.textSkill.text = lesson.skill
            binding.textSkill.visibleIf(lesson.skill.isNotBlank())
            binding.textTitle.text = lesson.title
            binding.textTitleGu.text = lesson.titleGu
            binding.textExplanation.text = lesson.explanation
            binding.textExplanationGu.text = lesson.explanationGu

            binding.examplesContainer.removeAllViews()
            binding.textExamplesHeader.visibleIf(lesson.examples.isNotEmpty())
            for (example in lesson.examples) {
                val item = ItemExampleBinding.inflate(layoutInflater, binding.examplesContainer, false)
                item.textEn.text = example.en
                item.textGu.text = example.gu
                item.textGu.visibleIf(example.gu.isNotBlank())
                item.btnPlay.setOnClickListener { speak(example.en) }
                binding.examplesContainer.addView(item.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
