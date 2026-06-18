package com.nhlstenden.momentum.data

import android.content.Context
import com.nhlstenden.momentum.ui.theme.MomentumAppTheme

object ThemeStore {
    private const val PREFS_NAME = "momentum_prefs"
    private const val KEY_THEME = "selected_theme"

    fun save(context: Context, theme: MomentumAppTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.storageValue)
            .apply()
    }

    fun load(context: Context): MomentumAppTheme {
        val storedValue = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        return MomentumAppTheme.fromStorageValue(storedValue)
    }
}
