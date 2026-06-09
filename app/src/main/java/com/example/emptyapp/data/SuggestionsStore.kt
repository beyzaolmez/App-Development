package com.example.emptyapp.data

import android.content.Context

// Persists user quest suggestions locally via SharedPreferences.
// Same prefs file as InterestsStore and StreakStore — all local data in one place.
// Each entry is stored as "title|category|notes" — simple enough for a Sprint 2 POC.
object SuggestionsStore {

    private const val PREFS_NAME = "momentum_prefs"
    private const val KEY_SUGGESTIONS = "quest_suggestions"

    fun save(context: Context, title: String, category: String, notes: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_SUGGESTIONS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        existing.add("${title.trim()}|${category.trim()}|${notes.trim()}")
        prefs.edit().putStringSet(KEY_SUGGESTIONS, existing).apply()
    }

    fun getAll(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SUGGESTIONS, emptySet()) ?: emptySet()
}
