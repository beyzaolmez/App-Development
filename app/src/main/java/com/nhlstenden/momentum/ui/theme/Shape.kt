package com.nhlstenden.momentum.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Brandbook v1.0 section 05 — border radius scale.
// Pills for chips/filters, larger rounding for cards.
val MomentumShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // default
    small = RoundedCornerShape(8.dp),        // lg
    medium = RoundedCornerShape(12.dp),      // xl — quest cards
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val PillShape = RoundedCornerShape(percent = 50)
