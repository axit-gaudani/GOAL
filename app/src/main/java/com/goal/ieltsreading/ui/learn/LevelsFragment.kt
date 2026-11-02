package com.goal.ieltsreading.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** The learning path: Level 0 to Level 6. */
class LevelsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LevelsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textHeader.text = getString(R.string.levels_header)
        // Gujarati line under the title, as in the Path design.
        binding.textSubHeader.text = getString(R.string.g2_levels_subheader)
        binding.textSubHeader.setTextColor(ContextCompat.getColor(requireContext(), R.color.gujarati_text))
        binding.groupHeaderActions.visibility = View.VISIBLE
        binding.btnHeaderAction.text = getString(R.string.title_sentence_practice)
        binding.btnHeaderAction.setOnClickListener { findNavController().navigate(R.id.sentenceHubFragment) }
        binding.btnHeaderAction2.text = getString(R.string.nav_tests)
        binding.btnHeaderAction2.setOnClickListener { findNavController().navigate(R.id.testsFragment) }
        val adapter = LevelsAdapter { item ->
            if (item.unlocked) {
                findNavController().navigate(R.id.levelDetailFragment, bundleOf("levelId" to item.level.id))
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.locked_level_title)
                    .setMessage(R.string.locked_level_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        binding.recycler.adapter = adapter
        viewModel.levels.observe(viewLifecycleOwner) { adapter.submitList(it) }
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
