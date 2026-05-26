package com.nhlstenden.momentum.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.InterestsStore
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.ui.theme.PillShape

// These labels are the canonical interest names — they must match
// the quest category strings in HomeScreen so filtering works correctly.
private val availableInterests = listOf(
    "Academic", "Social", "Personal",
    "Outdoors", "Creative", "Focus",
    "Wellness", "Food", "Reflection"
)

@Composable
fun InterestSelectionScreen(
    onContinue: () -> Unit = {}
) {
    val context = LocalContext.current
    // Pre-load any previously saved interests so the screen works as an editor too
    var selected by remember { mutableStateOf(InterestsStore.load(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "What are you into?",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Pick as many as you like — your quests will match.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Interest chip grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableInterests) { interest ->
                val isSelected = interest in selected
                InterestChip(
                    label = interest,
                    selected = isSelected,
                    onClick = {
                        selected = if (isSelected) selected - interest else selected + interest
                    }
                )
            }
        }

        // Continue — disabled until at least one interest is chosen
        MomentumPrimaryButton(
            text = "Continue",
            onClick = {
                InterestsStore.save(context, selected)
                onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.isNotEmpty()
        )
    }
}

// Private toggle chip — selected state uses primary colour, unselected is subtle.
@Composable
private fun InterestChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else
        MaterialTheme.colorScheme.surface

    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val textColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(containerColor, PillShape)
            .border(1.dp, borderColor, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun InterestSelectionScreenPreview() {
    MomentumTheme { InterestSelectionScreen() }
}
