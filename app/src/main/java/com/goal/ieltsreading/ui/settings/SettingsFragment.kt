package com.goal.ieltsreading.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentSettingsBinding
import com.goal.ieltsreading.reminder.ReminderScheduler
import com.goal.ieltsreading.ui.common.app
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val prefs get() = app.prefs

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableReminder() else {
                binding.switchReminder.isChecked = false
                Snackbar.make(binding.root, R.string.permission_needed, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.inputName.setText(prefs.userName)
        binding.inputName.doAfterTextChanged { prefs.userName = it?.toString()?.trim().orEmpty() }

        binding.goalChips.chipGroupGoal.check(
            when (prefs.dailyGoalMinutes) {
                10 -> R.id.chipGoal10
                20 -> R.id.chipGoal20
                30 -> R.id.chipGoal30
                else -> R.id.chipGoal15
            }
        )
        binding.goalChips.chipGroupGoal.setOnCheckedStateChangeListener { _, ids ->
            prefs.dailyGoalMinutes = when (ids.firstOrNull()) {
                R.id.chipGoal10 -> 10
                R.id.chipGoal20 -> 20
                R.id.chipGoal30 -> 30
                else -> 15
            }
        }

        binding.switchGujarati.isChecked = prefs.showGujarati
        binding.switchGujarati.setOnCheckedChangeListener { _, c -> prefs.showGujarati = c }

        binding.switchSkipAbc.isChecked = prefs.skipAbc
        binding.switchSkipAbc.setOnCheckedChangeListener { _, c -> prefs.skipAbc = c }

        binding.switchUnlockAll.isChecked = prefs.unlockAll
        binding.switchUnlockAll.setOnCheckedChangeListener { _, c -> prefs.unlockAll = c }

        binding.switchReminder.isChecked = prefs.reminderEnabled
        updateReminderTime()
        binding.switchReminder.setOnCheckedChangeListener { _, checked ->
            if (!checked) {
                prefs.reminderEnabled = false
                ReminderScheduler.cancel(requireContext())
            } else if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enableReminder()
            }
        }
        binding.rowReminderTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(prefs.reminderHour)
                .setMinute(prefs.reminderMinute)
                .setTitleText(R.string.reminder_time)
                .build()
            picker.addOnPositiveButtonClickListener {
                prefs.reminderHour = picker.hour
                prefs.reminderMinute = picker.minute
                updateReminderTime()
                if (prefs.reminderEnabled) ReminderScheduler.schedule(requireContext(), picker.hour, picker.minute)
            }
            picker.show(childFragmentManager, "time")
        }

        binding.btnReset.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_reset) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        app.repository.resetProgress()
                        _binding?.let { Snackbar.make(it.root, R.string.reset_done, Snackbar.LENGTH_SHORT).show() }
                    }
                }
                .show()
        }
    }

    private fun enableReminder() {
        prefs.reminderEnabled = true
        ReminderScheduler.schedule(requireContext(), prefs.reminderHour, prefs.reminderMinute)
        Snackbar.make(binding.root, R.string.reminder_set, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateReminderTime() {
        val h = prefs.reminderHour
        val m = prefs.reminderMinute
        val hour12 = if (h % 12 == 0) 12 else h % 12
        binding.textReminderTime.text = String.format("%d:%02d %s", hour12, m, if (h < 12) "AM" else "PM")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
