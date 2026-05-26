package com.nhlstenden.momentum.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable

// Brandbook v1.0 section 04 — 8-point base grid.
@Immutable
data class MomentumSpacing(
    val xs: Dp = 4.dp,
    val base: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val gutter: Dp = 16.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 32.dp,
    val xl: Dp = 48.dp
)

val LocalMomentumSpacing = staticCompositionLocalOf { MomentumSpacing() }
