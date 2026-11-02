package com.goal.ieltsreading.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.goal.ieltsreading.IeltsApp
import com.goal.ieltsreading.MainActivity
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.ActivityWelcomeBinding

/** First screen: explains the learning path and asks for a name and daily goal. */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = (application as IeltsApp).prefs

        binding.btnStart.setOnClickListener {
            prefs.userName = binding.inputName.text?.toString()?.trim().orEmpty()
            prefs.dailyGoalMinutes = when (binding.goalChips.chipGroupGoal.checkedChipId) {
                R.id.chipGoal10 -> 10
                R.id.chipGoal20 -> 20
                R.id.chipGoal30 -> 30
                else -> 15
            }
            // Where the learner starts: ABC (Level −1), Level 0, or every level open.
            when (binding.radioStart.checkedRadioButtonId) {
                R.id.radioKnowAbc -> { prefs.skipAbc = true; prefs.unlockAll = false }
                R.id.radioKnowBasics -> { prefs.skipAbc = true; prefs.unlockAll = true }
                else -> { prefs.skipAbc = false; prefs.unlockAll = false }
            }
            prefs.onboarded = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
