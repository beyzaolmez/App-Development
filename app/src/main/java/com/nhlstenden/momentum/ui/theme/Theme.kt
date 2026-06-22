package com.nhlstenden.momentum.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MomentumTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: MomentumAppTheme = MomentumAppTheme.Default,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeFor(appTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalMomentumSpacing provides MomentumSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MomentumTypography,
            shapes = MomentumShapes,
            content = content
        )
    }
}

// Convenience accessor: MaterialTheme.spacing
object MomentumThemeTokens {
    val spacing: MomentumSpacing
        @Composable get() = LocalMomentumSpacing.current
}
