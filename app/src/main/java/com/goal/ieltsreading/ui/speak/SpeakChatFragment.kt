package com.goal.ieltsreading.ui.speak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentSpeakChatBinding
import com.goal.ieltsreading.databinding.ItemChatPartnerBinding
import com.goal.ieltsreading.databinding.ItemChatYouBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.button.MaterialButton

/** A short scripted conversation. The partner speaks; the learner picks an answer and says it. */
class SpeakChatFragment : Fragment() {

    private var _binding: FragmentSpeakChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpeakChatViewModel by viewModels()
    private var showGujarati = false

    private val speechInput = SpeechInput(this) { text -> viewModel.answered(text) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpeakChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        val canCheck = speechInput.isAvailable(requireContext())
        viewModel.checkAvailable = canCheck
        showGujarati = app.prefs.showGujarati

        binding.btnHint.setOnClickListener { showGujarati = !showGujarati; viewModel.state.value?.let { render(it) } }
        binding.btnReplay.setOnClickListener {
            val last = viewModel.state.value?.lines?.lastOrNull { it is ChatLine.Partner } as? ChatLine.Partner
            last?.let { speak(it.en) }
        }
        binding.btnSpeak.setOnClickListener {
            val picked = viewModel.state.value?.picked ?: return@setOnClickListener
            if (canCheck) speechInput.listen(getString(R.string.speech_prompt_format, picked.en))
            else viewModel.answered(null)
        }
        binding.btnListenAnswer.setOnClickListener { viewModel.state.value?.picked?.let { speak(it.en) } }
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }

        viewModel.speak.observe(viewLifecycleOwner) { text ->
            if (text != null) {
                speak(text)
                viewModel.spoken()
            }
        }
        viewModel.state.observe(viewLifecycleOwner) { render(it) }
    }

    private fun render(s: ChatState) {
        binding.textPartner.text = s.partnerName
        binding.textAvatar.text = s.partnerName.take(1)
        binding.textTitle.text = s.title

        binding.chatContainer.removeAllViews()
        for (line in s.lines) {
            when (line) {
                is ChatLine.Partner -> {
                    val item = ItemChatPartnerBinding.inflate(layoutInflater, binding.chatContainer, false)
                    item.textEn.text = line.en
                    item.textGu.text = line.gu
                    item.textGu.visibleIf(showGujarati)
                    item.root.setOnClickListener { speak(line.en) }
                    binding.chatContainer.addView(item.root)
                }
                is ChatLine.You -> {
                    val item = ItemChatYouBinding.inflate(layoutInflater, binding.chatContainer, false)
                    item.textEn.text = line.en
                    item.textStatus.visibleIf(line.clear != null)
                    item.textStatus.setText(if (line.clear == true) R.string.speech_clear_short else R.string.speech_try_next_time)
                    binding.chatContainer.addView(item.root)
                }
            }
        }
        binding.scroll.post { _binding?.scroll?.fullScroll(View.FOCUS_DOWN) }

        binding.textChoose.visibleIf(s.options.isNotEmpty())
        binding.optionsContainer.visibleIf(s.options.isNotEmpty())
        binding.optionsContainer.removeAllViews()
        for (o in s.options) {
            val button = layoutInflater.inflate(R.layout.view_option_button, binding.optionsContainer, false) as MaterialButton
            button.text = if (showGujarati) "${o.en}\n${o.gu}" else o.en
            button.setOnClickListener { viewModel.pick(o) }
            binding.optionsContainer.addView(button)
        }

        val picked = s.picked
        binding.cardSay.visibleIf(picked != null)
        if (picked != null) {
            binding.textSay.text = picked.en
            binding.textSayGu.text = picked.gu
            binding.textSayGu.visibleIf(showGujarati)
        }
        binding.groupControls.visibleIf(!s.finished)
        binding.btnSpeak.isEnabled = picked != null
        binding.groupFinished.visibleIf(s.finished)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
