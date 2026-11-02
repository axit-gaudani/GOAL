package com.goal.ieltsreading.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.goal.ieltsreading.IeltsApp
import com.goal.ieltsreading.MainActivity
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.ActivityWelcomeBinding

/**
 * Onboarding in two steps:
 * 1. Welcome — what the app does and the learner's name.
 * 2. Placement — how much English the learner knows (sets where they start) and the daily goal.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    /** Where the learner says they are. */
    private enum class Start { NOTHING, KNOW_ABC, SENTENCES, UNSURE }

    private var start = Start.NOTHING
    private var step = 1

    private val backToWelcome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = showStep(1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, backToWelcome)

        val prefs = (application as IeltsApp).prefs

        val options = mapOf(
            Start.NOTHING to (binding.optNothing to binding.checkNothing),
            Start.KNOW_ABC to (binding.optKnowAbc to binding.checkKnowAbc),
            Start.SENTENCES to (binding.optSentences to binding.checkSentences),
            Start.UNSURE to (binding.optUnsure to binding.checkUnsure)
        )
        fun select(s: Start) {
            start = s
            options.forEach { (key, views) ->
                val (card, check) = views
                card.isSelected = key == s
                check.visibility = if (key == s) View.VISIBLE else View.GONE
            }
        }
        options.forEach { (key, views) -> views.first.setOnClickListener { select(key) } }

        savedInstanceState?.let {
            start = Start.entries.getOrNull(it.getInt(KEY_START)) ?: Start.NOTHING
            step = it.getInt(KEY_STEP, 1)
        }
        select(start)
        showStep(step)

        binding.textStep.text = getString(R.string.g1_step_format, 2, 2)
        binding.btnStart.setOnClickListener { showStep(2) }
        binding.btnBack.setOnClickListener { showStep(1) }

        binding.btnContinue.setOnClickListener {
            prefs.userName = binding.inputName.text?.toString()?.trim().orEmpty()
            prefs.dailyGoalMinutes = when (binding.goalChips.chipGroupGoal.checkedChipId) {
                R.id.chipGoal10 -> 10
                R.id.chipGoal20 -> 20
                R.id.chipGoal30 -> 30
                else -> 15
            }
            // ABC is Level −1. "Simple sentences" also starts at Level 0 (no level is skipped beyond ABC).
            // "Not sure" starts from the beginning because the app has no placement test.
            when (start) {
                Start.KNOW_ABC, Start.SENTENCES -> { prefs.skipAbc = true; prefs.unlockAll = false }
                Start.NOTHING, Start.UNSURE -> { prefs.skipAbc = false; prefs.unlockAll = false }
            }
            prefs.onboarded = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showStep(s: Int) {
        step = s
        binding.stepWelcome.visibility = if (s == 1) View.VISIBLE else View.GONE
        binding.stepPlacement.visibility = if (s == 2) View.VISIBLE else View.GONE
        backToWelcome.isEnabled = s == 2
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_START, start.ordinal)
        outState.putInt(KEY_STEP, step)
    }

    private companion object {
        const val KEY_START = "start"
        const val KEY_STEP = "step"
    }
}
