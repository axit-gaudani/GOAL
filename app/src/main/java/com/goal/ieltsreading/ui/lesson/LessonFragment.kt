package com.goal.ieltsreading.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.tabs.TabLayoutMediator

/**
 * One lesson in 4 steps:
 * 1. Learn (explanation + Gujarati)  2. Words  3. Read the passage  4. Practice questions
 */
class LessonFragment : Fragment() {

    private var _binding: FragmentLessonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))

        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> LessonLearnFragment()
                1 -> LessonWordsFragment()
                else -> LessonReadFragment()
            }
        }
        val titles = listOf(R.string.tab_learn, R.string.tab_words, R.string.tab_read)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position -> tab.setText(titles[position]) }.attach()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                _binding?.btnNext?.setText(if (position < 2) R.string.btn_next_step else R.string.btn_start_practice)
            }
        })
        binding.btnNext.setOnClickListener {
            val page = binding.pager.currentItem
            if (page < 2) {
                binding.pager.currentItem = page + 1
            } else {
                findNavController().navigate(R.id.quizFragment, bundleOf("lessonId" to viewModel.lessonId))
            }
        }

        viewModel.lesson.observe(viewLifecycleOwner) { lesson ->
            if (lesson == null) return@observe
            (activity as? AppCompatActivity)?.supportActionBar?.title = lesson.title
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
