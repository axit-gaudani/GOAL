package com.goal.ieltsreading.data.prefs

import android.content.Context
import androidx.core.content.edit

/** Small user settings stored in SharedPreferences. */
class UserPrefs(context: Context) {
    private val sp = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit { putBoolean("onboarded", v) }

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(v) = sp.edit { putString("user_name", v) }

    /** Daily study goal in minutes. */
    var dailyGoalMinutes: Int
        get() = sp.getInt("daily_goal", 15)
        set(v) = sp.edit { putInt("daily_goal", v) }

    /** Show Gujarati translations open by default. */
    var showGujarati: Boolean
        get() = sp.getBoolean("show_gujarati", true)
        set(v) = sp.edit { putBoolean("show_gujarati", v) }

    /** Lets students who already know the basics open every level. */
    var unlockAll: Boolean
        get() = sp.getBoolean("unlock_all", false)
        set(v) = sp.edit { putBoolean("unlock_all", v) }

    /** The learner already knows the English letters, so Level −1 (ABC) is optional. */
    var skipAbc: Boolean
        get() = sp.getBoolean("skip_abc", false)
        set(v) = sp.edit { putBoolean("skip_abc", v) }

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", false)
        set(v) = sp.edit { putBoolean("reminder_enabled", v) }

    var reminderHour: Int
        get() = sp.getInt("reminder_hour", 19)
        set(v) = sp.edit { putInt("reminder_hour", v) }

    var reminderMinute: Int
        get() = sp.getInt("reminder_minute", 0)
        set(v) = sp.edit { putInt("reminder_minute", v) }

    /** Word pack chosen in the Vocabulary builder. */
    var currentPackId: String
        get() = sp.getString("current_pack", "") ?: ""
        set(v) = sp.edit { putString("current_pack", v) }

    /** Speak words automatically when a word card opens. */
    var autoSpeak: Boolean
        get() = sp.getBoolean("auto_speak", true)
        set(v) = sp.edit { putBoolean("auto_speak", v) }

    var contentVersion: Int
        get() = sp.getInt("content_version", 0)
        set(v) = sp.edit { putInt("content_version", v) }
}
