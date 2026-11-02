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
import com.goal.ieltsreading.databinding.FragmentListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TestsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TestsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textHeader.text = getString(R.string.tests_header)
        binding.textSubHeader.text = getString(R.string.tests_subheader)
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
        viewModel.tests.observe(viewLifecycleOwner) { adapter.submitList(it) }
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
