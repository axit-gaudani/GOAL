package com.goal.ieltsreading.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentListBinding
import com.google.android.material.snackbar.Snackbar

/** All lessons of one level. */
class LevelDetailFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LevelDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = LessonsAdapter { item ->
            if (item.unlocked) {
                findNavController().navigate(R.id.lessonFragment, bundleOf("lessonId" to item.lesson.id))
            } else {
                Snackbar.make(binding.root, R.string.locked_lesson_message, Snackbar.LENGTH_LONG).show()
            }
        }
        binding.recycler.adapter = adapter
        viewModel.level.observe(viewLifecycleOwner) { level ->
            if (level == null) return@observe
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.level_title_format, level.sortOrder, level.title)
            binding.textHeader.text = level.band
            binding.textSubHeader.text = "${level.description}\n${level.descriptionGu}"
        }
        viewModel.lessons.observe(viewLifecycleOwner) { adapter.submitList(it) }
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
