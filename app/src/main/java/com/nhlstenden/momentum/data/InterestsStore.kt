package com.nhlstenden.momentum.data

import android.content.Context

object InterestsStore {
    private const val PREFS_NAME = "momentum_prefs"
    private const val KEY_INTERESTS = "selected_interests"

    fun save(context: Context, userId: String?, interests: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(keyFor(userId), interests)
            .apply()
    }

    fun load(context: Context, userId: String?): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(keyFor(userId), emptySet())
            .orEmpty()

    fun hasInterests(context: Context, userId: String?): Boolean =
        load(context, userId).isNotEmpty()

    fun clear(context: Context, userId: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(keyFor(userId))
            .apply()
    }

    private fun keyFor(userId: String?): String =
        if (userId.isNullOrBlank()) KEY_INTERESTS else "${KEY_INTERESTS}_$userId"
}
