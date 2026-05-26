package com.nhlstenden.momentum.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Momentum is dark-only by design (brandbook section 08: "Dark first always").
private val MomentumColorScheme = darkColorScheme(
    primary = MomentumPrimary,
    onPrimary = MomentumOnPrimary,
    primaryContainer = MomentumPrimaryContainer,
    onPrimaryContainer = MomentumOnPrimaryContainer,
    secondary = MomentumSecondary,
    onSecondary = MomentumOnSecondary,
    tertiary = MomentumTertiary,
    onTertiary = MomentumOnTertiary,
    background = MomentumBackground,
    onBackground = MomentumOnSurface,
    surface = MomentumSurface,
    onSurface = MomentumOnSurface,
    surfaceVariant = MomentumSurfaceHigh,
    onSurfaceVariant = MomentumOnSurfaceVariant,
    outline = MomentumOutline,
    outlineVariant = MomentumOutlineVariant
)

@Composable
fun MomentumTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MomentumColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalMomentumSpacing provides MomentumSpacing()) {
        MaterialTheme(
            colorScheme = MomentumColorScheme,
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
