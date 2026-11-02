package com.goal.ieltsreading.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentTestsBinding
import com.goal.ieltsreading.domain.BandCalculator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TestsFragment : Fragment() {

    private var _binding: FragmentTestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TestsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = TestsAdapter { item ->
            if (!item.unlocked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.locked_test_title)
                    .setMessage(getString(R.string.locked_test_message, item.requiredLevelTitle))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@TestsAdapter
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.test.title)
                .setMessage(getString(R.string.test_start_message, item.test.minutes))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_start_test) { _, _ ->
                    findNavController().navigate(R.id.testFragment, bundleOf("testId" to item.test.id))
                }
                .show()
        }
        binding.recycler.adapter = adapter
        viewModel.tests.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            // "Your best band" card: the best band over all practice tests taken.
            val best = list.mapNotNull { it.bestBand }.maxOrNull()
            binding.textBestBand.text = best?.let { BandCalculator.format(it) } ?: getString(R.string.g3_dash)
            binding.textBestSub.setText(if (best != null) R.string.g3_best_band_sub else R.string.g3_best_band_none)
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
