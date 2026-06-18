package com.nhlstenden.momentum.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

enum class MomentumAppTheme(
    val label: String,
    val storageValue: String,
    val previewColors: List<Color>
) {
    Classic(
        label = "Classic",
        storageValue = "classic",
        previewColors = listOf(MomentumPrimary, MomentumSecondary, MomentumTertiary)
    ),
    Ocean(
        label = "Ocean",
        storageValue = "ocean",
        previewColors = listOf(Color(0xFF8FD8FF), Color(0xFF7EE7D3), Color(0xFFB5C7FF))
    ),
    Forest(
        label = "Forest",
        storageValue = "forest",
        previewColors = listOf(Color(0xFFA7E8C2), Color(0xFF9FD4B3), Color(0xFFD7C99A))
    ),
    Sunset(
        label = "Sunset",
        storageValue = "sunset",
        previewColors = listOf(Color(0xFFFFC6A6), Color(0xFFD9B7FF), Color(0xFFFFD889))
    ),
    Paper(
        label = "Paper",
        storageValue = "paper",
        previewColors = listOf(Color(0xFF2F6F73), Color(0xFF8D79B8), Color(0xFFD9A441))
    );

    companion object {
        val Default = Classic

        fun fromStorageValue(value: String?): MomentumAppTheme =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

fun colorSchemeFor(theme: MomentumAppTheme): ColorScheme =
    when (theme) {
        MomentumAppTheme.Classic -> darkColorScheme(
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

        MomentumAppTheme.Ocean -> darkColorScheme(
            primary = Color(0xFF8FD8FF),
            onPrimary = Color(0xFF051522),
            primaryContainer = Color(0xFF2A6F97),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF7EE7D3),
            onSecondary = Color(0xFF041915),
            tertiary = Color(0xFFB5C7FF),
            onTertiary = Color(0xFF081225),
            background = Color(0xFF07121C),
            onBackground = Color(0xFFDCEAF4),
            surface = Color(0xFF122231),
            onSurface = Color(0xFFDCEAF4),
            surfaceVariant = Color(0xFF1E3445),
            onSurfaceVariant = Color(0xFFC0D1DD),
            outline = Color(0xFF89A2B2),
            outlineVariant = Color(0xFF3A5364)
        )

        MomentumAppTheme.Forest -> darkColorScheme(
            primary = Color(0xFFA7E8C2),
            onPrimary = Color(0xFF06180F),
            primaryContainer = Color(0xFF3E7E5A),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF9FD4B3),
            onSecondary = Color(0xFF07170D),
            tertiary = Color(0xFFD7C99A),
            onTertiary = Color(0xFF1A1607),
            background = Color(0xFF09130E),
            onBackground = Color(0xFFE0EADF),
            surface = Color(0xFF16231A),
            onSurface = Color(0xFFE0EADF),
            surfaceVariant = Color(0xFF243429),
            onSurfaceVariant = Color(0xFFC6D5C9),
            outline = Color(0xFF91A091),
            outlineVariant = Color(0xFF445447)
        )

        MomentumAppTheme.Sunset -> darkColorScheme(
            primary = Color(0xFFFFC6A6),
            onPrimary = Color(0xFF241207),
            primaryContainer = Color(0xFF8B6A3E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFD9B7FF),
            onSecondary = Color(0xFF1B1026),
            tertiary = Color(0xFFFFD889),
            onTertiary = Color(0xFF1E1604),
            background = Color(0xFF19121B),
            onBackground = Color(0xFFF0E2EA),
            surface = Color(0xFF281E2B),
            onSurface = Color(0xFFF0E2EA),
            surfaceVariant = Color(0xFF382C3B),
            onSurfaceVariant = Color(0xFFD8C8D7),
            outline = Color(0xFFA797A8),
            outlineVariant = Color(0xFF55465A)
        )

        MomentumAppTheme.Paper -> darkColorScheme(
            primary = Color(0xFF2F6F73),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF2F6F73),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF8D79B8),
            onSecondary = Color(0xFFFFFFFF),
            tertiary = Color(0xFFD9A441),
            onTertiary = Color(0xFF231703),
            background = Color(0xFFF7F5EF),
            onBackground = Color(0xFF202733),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF202733),
            surfaceVariant = Color(0xFFE9E6DE),
            onSurfaceVariant = Color(0xFF56606A),
            outline = Color(0xFF8A9298),
            outlineVariant = Color(0xFFD4D0C7)
        )
    }
