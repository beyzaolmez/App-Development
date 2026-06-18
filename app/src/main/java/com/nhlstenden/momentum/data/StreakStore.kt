package com.nhlstenden.momentum.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Tracks the user's soft streak — consecutive days with at least one quest completed.
// Uses the same SharedPreferences file as InterestsStore so all local data stays together.
// SimpleDateFormat + Calendar used instead of java.time.LocalDate for API 24 compatibility.
object StreakStore {

    private const val PREFS_NAME = "momentum_prefs"
    private const val KEY_STREAK = "streak_count"
    private const val KEY_LAST_DATE = "last_completed_date"

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private fun today(): String = dateFormat.get()!!.format(Calendar.getInstance().time)

    private fun yesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.get()!!.format(cal.time)
    }

    // Call this when a quest is completed.
    // G5-78: increments on a new day.
    // G5-79: resets to 1 when a day has been missed (gap > 1 day).
    fun recordCompletion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val today = today()

        val newStreak = when {
            lastDate == today -> prefs.getInt(KEY_STREAK, 1)      // already counted today
            lastDate == yesterday() -> prefs.getInt(KEY_STREAK, 0) + 1  // consecutive day
            else -> 1                                               // first ever, or gap → reset
        }

        prefs.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_DATE, today)
            .apply()
    }

    fun getStreak(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_STREAK, 0)
}
