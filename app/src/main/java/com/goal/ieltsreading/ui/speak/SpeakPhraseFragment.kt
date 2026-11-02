package com.goal.ieltsreading.ui.speak

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.audio.VoiceRecorder
import com.goal.ieltsreading.data.content.SpeakingPhrase
import com.goal.ieltsreading.databinding.FragmentSpeakPhraseBinding
import com.goal.ieltsreading.ui.common.StudyTimeTracker
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.speak
import com.goal.ieltsreading.ui.common.visibleIf
import com.google.android.material.snackbar.Snackbar

/** Listen to a phrase, say it, check what the phone heard, and compare with your own recording. */
class SpeakPhraseFragment : Fragment() {

    private var _binding: FragmentSpeakPhraseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpeakPhraseViewModel by viewModels()
    private var recorder: VoiceRecorder? = null
    private var shownIndex = -1

    private val speechInput = SpeechInput(this) { text ->
        if (text != null) viewModel.onHeard(text)
        else _binding?.let { Snackbar.make(it.root, R.string.speech_not_heard, Snackbar.LENGTH_SHORT).show() }
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleRecording()
        else _binding?.let { Snackbar.make(it.root, R.string.mic_permission_needed, Snackbar.LENGTH_LONG).show() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpeakPhraseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(StudyTimeTracker(app))
        recorder = VoiceRecorder(requireContext())
        shownIndex = -1
        val canCheck = speechInput.isAvailable(requireContext())
        viewModel.checkAvailable = canCheck
        binding.btnCheck.visibleIf(canCheck)
        binding.textNoCheck.visibleIf(!canCheck)

        binding.btnClose.setOnClickListener { findNavController().navigateUp() }
        binding.btnTeacher.setOnClickListener { viewModel.state.value?.let { speak(it.phrase.en) } }
        binding.btnSlow.setOnClickListener { viewModel.state.value?.let { speak(it.phrase.en, slow = true) } }
        binding.btnMyVoice.setOnClickListener { recorder?.play() }
        binding.btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) toggleRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        binding.btnCheck.setOnClickListener {
            viewModel.state.value?.let { speechInput.listen(getString(R.string.speech_prompt_format, it.phrase.en)) }
        }
        binding.btnNext.setOnClickListener {
            stopRecording()
            viewModel.next()
        }
        binding.btnToChat.setOnClickListener {
            findNavController().navigate(
                R.id.speakChatFragment, bundleOf("unitId" to viewModel.unitId),
                NavOptions.Builder().setPopUpTo(R.id.speakPhraseFragment, true).build()
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { s ->
            if (s == null) return@observe
            binding.groupPhrase.visibleIf(!s.finished)
            binding.groupFinished.visibleIf(s.finished)
            if (s.finished) {
                binding.textFinished.text = if (viewModel.checkAvailable)
                    getString(R.string.speak_phrases_done, s.clearCount, s.total)
                else getString(R.string.speak_phrases_done_nocheck, s.total)
                return@observe
            }
            binding.textUnit.text = s.unitTitle
            binding.textCounter.text = getString(R.string.counter_format, s.index + 1, s.total)
            binding.progress.max = s.total
            binding.progress.setProgressCompat(s.index, true)
            binding.textPhrase.text = stressed(s.phrase)
            binding.textPron.text = s.phrase.pronGu
            binding.textGu.text = s.phrase.gu
            if (shownIndex != s.index) {
                shownIndex = s.index
                recorder?.clear()
                binding.btnMyVoice.isEnabled = false
                speak(s.phrase.en)
            }

            val m = s.match
            binding.cardHeard.visibleIf(m != null)
            if (m != null) {
                binding.textHeardTitle.setText(if (m.clear) R.string.speech_clear else R.string.speech_try_again)
                binding.textHeardTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), if (m.clear) R.color.success else R.color.error)
                )
                binding.textHeard.text = heardText(m.words)
                binding.textHeardCount.text = getString(R.string.speech_words_clear, m.matched, m.total)
            }
            binding.btnNext.setText(if (s.index + 1 >= s.total) R.string.btn_finish else R.string.btn_next_phrase)
        }
    }

    /** Underlines and bolds the stressed words. */
    private fun stressed(phrase: SpeakingPhrase): CharSequence {
        val sb = SpannableStringBuilder()
        val color = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        phrase.en.split(" ").forEachIndexed { i, word ->
            if (i > 0) sb.append(" ")
            val start = sb.length
            sb.append(word)
            val clean = word.trim('.', ',', '!', '?', ';', ':', '"', '\'')
            if (phrase.stress.any { it.equals(clean, ignoreCase = true) }) {
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(UnderlineSpan(), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    /** Words that were not heard get a yellow background. */
    private fun heardText(words: List<Pair<String, Boolean>>): CharSequence {
        val sb = SpannableStringBuilder()
        val missing = ContextCompat.getColor(requireContext(), R.color.brand_secondary_container)
        words.forEachIndexed { i, (word, ok) ->
            if (i > 0) sb.append(" ")
            val start = sb.length
            sb.append(word)
            if (!ok) sb.setSpan(BackgroundColorSpan(missing), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun toggleRecording() {
        val r = recorder ?: return
        if (r.isRecording) {
            stopRecording()
            viewModel.onRecorded()
        } else if (r.start()) {
            binding.btnRecord.setText(R.string.btn_stop_recording)
            binding.btnRecord.setIconResource(R.drawable.ic_stop)
        }
    }

    private fun stopRecording() {
        recorder?.stop()
        _binding?.btnRecord?.setText(R.string.btn_record)
        _binding?.btnRecord?.setIconResource(R.drawable.ic_mic)
        _binding?.btnMyVoice?.isEnabled = recorder?.hasRecording == true
    }

    override fun onPause() {
        super.onPause()
        if (recorder?.isRecording == true) stopRecording()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recorder?.release()
        recorder = null
        _binding = null
    }
}
