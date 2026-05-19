package com.example.emptyapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.ui.theme.MomentumTheme

// Brandbook §06 card anatomy:
// • surface bg (#171F33), 1px outline-variant border, 12dp radius.
// Generic container — pass any content via the trailing lambda.
@Composable
fun MomentumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable (Modifier) -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(
        modifier = modifier.fillMaxWidth().then(clickableModifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        content(Modifier.padding(contentPadding))
    }
}

// QuestCard composes MomentumCard with the standard layout from the wireframe:
// category chip + optional difficulty, title, description, XP reward chip.
@Composable
fun QuestCard(
    title: String,
    description: String,
    category: String,
    xp: Int,
    modifier: Modifier = Modifier,
    difficulty: String? = null,
    onClick: (() -> Unit)? = null
) {
    MomentumCard(modifier = modifier, onClick = onClick) { inner ->
        Column(
            modifier = inner,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MomentumChip(category, variant = ChipVariant.Category)
                if (difficulty != null) {
                    Text(
                        difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                MomentumChip("+$xp XP", variant = ChipVariant.Reward)
            }
        }
    }
}

@Preview(name = "Card gallery", showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360)
@Composable
private fun CardGalleryPreview() {
    MomentumTheme {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuestCard(
                title = "Read Chapter 4",
                description = "Finish the assigned reading for History 101 before tomorrow's seminar.",
                category = "Academic",
                xp = 150,
                difficulty = "Medium"
            )
            QuestCard(
                title = "Master Python Basics",
                description = "Complete the first three modules of the introductory Python course.",
                category = "Skills",
                xp = 300,
                difficulty = "Hard"
            )
            MomentumCard {
                Column(it, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Soft streak", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "4 days · keep going gently",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
