package com.nhlstenden.momentum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.ui.theme.PillShape

// Brandbook §06: active = primary border + 10% primary fill. Uppercase, letter-spaced.
enum class ChipVariant { Category, Skills, Reward, Status, Neutral }

@Composable
fun MomentumChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Category
) {
    val container: Color
    val content: Color
    val border: Color
    when (variant) {
        ChipVariant.Category -> {
            container = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            content = MaterialTheme.colorScheme.primary
            border = MaterialTheme.colorScheme.primary
        }
        ChipVariant.Skills -> {
            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            content = MaterialTheme.colorScheme.secondary
            border = MaterialTheme.colorScheme.secondary
        }
        ChipVariant.Reward -> {
            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
            content = MaterialTheme.colorScheme.tertiary
            border = Color.Transparent
        }
        ChipVariant.Status -> {
            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            content = MaterialTheme.colorScheme.secondary
            border = Color.Transparent
        }
        ChipVariant.Neutral -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
            border = MaterialTheme.colorScheme.outlineVariant
        }
    }

    val baseModifier = modifier
        .clip(PillShape)
        .background(container, PillShape)
        .let { if (border != Color.Transparent) it.border(1.dp, border, PillShape) else it }
        .padding(horizontal = 10.dp, vertical = 4.dp)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text.uppercase(),
            color = content,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Chip gallery", showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360)
@Composable
private fun ChipGalleryPreview() {
    MomentumTheme {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MomentumChip("Academic", variant = ChipVariant.Category)
            MomentumChip("Skills", variant = ChipVariant.Skills)
            MomentumChip("+150 XP", variant = ChipVariant.Reward)
            MomentumChip("Medium", variant = ChipVariant.Neutral)
        }
    }
}
