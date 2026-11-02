package com.goal.ieltsreading.ui.lesson

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentLessonBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.google.android.material.button.MaterialButton

/**
 * One lesson in 4 steps:
 * 1. Learn (explanation + Gujarati)  2. Words  3. Read the passage  4. Practice questions
 * The steps are shown as a row of rounded buttons; the first three drive the pager,
 * the last one opens the practice questions.
 */
class LessonFragment : Fragment() {

    private var _binding: FragmentLessonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()
    private val stepButtons = mutableListOf<MaterialButton>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))

        // ABC lessons get an extra "Letters" step after Learn.
        val hasLetters = viewModel.letters.isNotEmpty()
        val pages = if (hasLetters) listOf(Page.LEARN, Page.LETTERS, Page.WORDS, Page.READ)
        else listOf(Page.LEARN, Page.WORDS, Page.READ)
        val last = pages.lastIndex
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = pages.size
            override fun createFragment(position: Int): Fragment = when (pages[position]) {
                Page.LEARN -> LessonLearnFragment()
                Page.LETTERS -> LessonLettersFragment()
                Page.WORDS -> LessonWordsFragment()
                Page.READ -> LessonReadFragment()
            }
        }

        // Step buttons: one per page, then "Practice".
        stepButtons.clear()
        binding.stepBar.removeAllViews()
        val titles = pages.map { it.title } + R.string.g2_step_practice
        titles.forEachIndexed { index, title ->
            val button = layoutInflater.inflate(R.layout.view_g2_step_button, binding.stepBar, false) as MaterialButton
            button.text = getString(R.string.g2_step_format, index + 1, getString(title))
            button.setOnClickListener {
                if (index <= last) binding.pager.currentItem = index else startPractice()
            }
            binding.stepBar.addView(button)
            stepButtons.add(button)
        }
        styleSteps(binding.pager.currentItem)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                _binding?.btnNext?.setText(if (position < last) R.string.g2_btn_next_step else R.string.btn_start_practice)
                styleSteps(position)
            }
        })
        binding.btnNext.setOnClickListener {
            val page = binding.pager.currentItem
            if (page < last) {
                binding.pager.currentItem = page + 1
            } else {
                startPractice()
            }
        }

        viewModel.lesson.observe(viewLifecycleOwner) { lesson ->
            if (lesson == null) return@observe
            (activity as? AppCompatActivity)?.supportActionBar?.title = lesson.title
        }
    }

    private fun startPractice() {
        findNavController().navigate(R.id.quizFragment, bundleOf("lessonId" to viewModel.lessonId))
    }

    /** Finished steps are green-soft, the current one is blue, the rest are white. */
    private fun styleSteps(current: Int) {
        val ctx = context ?: return
        val bold = ResourcesCompat.getFont(ctx, R.font.inter_bold)
        val semibold = ResourcesCompat.getFont(ctx, R.font.inter_semibold)
        stepButtons.forEachIndexed { index, button ->
            val (bg, fg) = when {
                index < current -> R.color.g2_correct_soft to R.color.g2_correct_text
                index == current -> R.color.brand_primary to R.color.white
                else -> R.color.surface to R.color.text_secondary
            }
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
            button.setTextColor(ContextCompat.getColor(ctx, fg))
            button.typeface = if (index <= current) bold else semibold
            button.isSelected = index == current
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stepButtons.clear()
        _binding = null
    }
}

private enum class Page(val title: Int) {
    LEARN(R.string.g2_step_learn),
    LETTERS(R.string.g2_step_abc),
    WORDS(R.string.g2_step_words),
    READ(R.string.g2_step_read)
}
