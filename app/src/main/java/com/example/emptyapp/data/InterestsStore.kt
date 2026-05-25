package com.example.emptyapp.data

import android.content.Context

// Persists the user's selected interest categories using SharedPreferences.
// No external dependency needed — correct scope for Sprint 1 POC.
object InterestsStore {

    private const val PREFS_NAME = "momentum_prefs"
    private const val KEY_INTERESTS = "selected_interests"

    fun save(context: Context, interests: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_INTERESTS, interests)
            .apply()
    }

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_INTERESTS, emptySet()) ?: emptySet()

    fun hasInterests(context: Context): Boolean = load(context).isNotEmpty()
}
