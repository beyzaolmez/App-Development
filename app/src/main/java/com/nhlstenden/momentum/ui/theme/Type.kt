package com.nhlstenden.momentum.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nhlstenden.momentum.R

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val SpaceGrotesk = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleFontsProvider, weight = FontWeight.Bold)
)

private val Lexend = FontFamily(
    Font(googleFont = GoogleFont("Lexend"), fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Lexend"), fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Lexend"), fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Lexend"), fontProvider = googleFontsProvider, weight = FontWeight.Bold)
)

// Mapping derived directly from brandbook v1.0 section 03.
val MomentumTypography = Typography(
    // Display Large — Space Grotesk 40 / 700 / 0.02em
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.02.em
    ),
    // Headline Large — Space Grotesk 32 / 600 / 0.01em
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.01.em
    ),
    // Headline Medium — Space Grotesk 24 / 600
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    // Title — Space Grotesk 18 / 600 (used for card titles like "Read Chapter 4")
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // Body Large — Lexend 18 / 400
    bodyLarge = TextStyle(
        fontFamily = Lexend,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    // Body Medium — Lexend 16 / 400
    bodyMedium = TextStyle(
        fontFamily = Lexend,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Label Large — Lexend 14 / 600 / +0.05em (used for category chips, uppercase)
    labelLarge = TextStyle(
        fontFamily = Lexend,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.em
    ),
    // Label Small — Lexend 12 / 500 (used for XP, difficulty, metadata)
    labelSmall = TextStyle(
        fontFamily = Lexend,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
