package com.nhlstenden.momentum.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MomentumAppThemeTest {

    @Test
    fun fromStorageValueResolvesKnownThemes() {
        MomentumAppTheme.entries.forEach { theme ->
            assertEquals(theme, MomentumAppTheme.fromStorageValue(theme.storageValue))
        }
    }

    @Test
    fun fromStorageValueFallsBackToDefaultForMissingOrUnknownValues() {
        assertEquals(MomentumAppTheme.Default, MomentumAppTheme.fromStorageValue(null))
        assertEquals(MomentumAppTheme.Default, MomentumAppTheme.fromStorageValue("unknown-theme"))
    }
}
