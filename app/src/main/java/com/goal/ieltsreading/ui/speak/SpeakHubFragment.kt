package com.goal.ieltsreading.ui.speak

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
import com.goal.ieltsreading.databinding.FragmentSpeakHubBinding
import com.goal.ieltsreading.databinding.ItemSpeakUnitBinding
import com.goal.ieltsreading.ui.common.G4Ui
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Basic speaking: phrase of the day and the 7 units. */
class SpeakHubFragment : Fragment() {

    private var _binding: FragmentSpeakHubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpeakHubViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpeakHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { s ->
            val phrase = s.phrase
            binding.cardPhrase.visibleIf(phrase != null)
            if (phrase != null) {
                binding.textPhrase.text = phrase.en
                binding.textPhraseGu.text = phrase.gu
                binding.textPhrasePron.text = phrase.pronGu
                binding.btnPhraseListen.setOnClickListener { speak(phrase.en) }
                // "Say it" opens the phrase step of the unit this phrase comes from
                val home = s.units.firstOrNull { it.unlocked && phrase in it.unit.phrases }
                binding.btnPhraseSay.visibleIf(home != null)
                binding.btnPhraseSay.setOnClickListener {
                    if (home != null) findNavController().navigate(R.id.speakPhraseFragment, bundleOf("unitId" to home.unit.id))
                }
            }

            binding.unitsContainer.removeAllViews()
            for (u in s.units) {
                val item = ItemSpeakUnitBinding.inflate(layoutInflater, binding.unitsContainer, false)
                item.textNumber.text = u.unit.order.toString()
                item.textTitle.text = u.unit.title
                item.textTitleGu.text = u.unit.titleGu
                val p = u.progress
                item.textMeta.text = when {
                    u.completed -> getString(R.string.status_done)
                    !u.unlocked -> getString(R.string.status_locked)
                    p != null -> getString(R.string.speak_progress_format, p.phrasesClear, u.unit.phrases.size)
                    else -> getString(R.string.status_ready)
                }
                val ctx = requireContext()
                val current = u.unlocked && !u.completed
                item.textNumber.setBackgroundResource(
                    when {
                        u.completed -> R.drawable.g4_bg_circle_green
                        u.unlocked -> R.drawable.g4_bg_circle_teal
                        else -> R.drawable.g4_bg_circle_white
                    }
                )
                item.textNumber.setTextColor(ContextCompat.getColor(ctx, if (u.unlocked) R.color.g4_on_color else R.color.text_secondary))
                item.root.setCardBackgroundColor(ContextCompat.getColor(ctx, if (u.unlocked) R.color.surface else R.color.surface_variant))
                item.root.strokeWidth = if (current) G4Ui.dp(ctx, 2f) else 0
                item.root.alpha = if (u.unlocked) 1f else 0.8f
                item.root.setOnClickListener {
                    if (!u.unlocked) {
                        Snackbar.make(binding.root, R.string.speak_unit_locked, Snackbar.LENGTH_LONG).show()
                    } else {
                        chooseStep(u.unit.id, u.unit.title)
                    }
                }
                binding.unitsContainer.addView(item.root)
            }
        }
    }

    private fun chooseStep(unitId: String, title: String) {
        val steps = arrayOf(getString(R.string.speak_step_phrases), getString(R.string.speak_step_chat))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(steps) { _, which ->
                val dest = if (which == 0) R.id.speakPhraseFragment else R.id.speakChatFragment
                findNavController().navigate(dest, bundleOf("unitId" to unitId))
            }
            .show()
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
