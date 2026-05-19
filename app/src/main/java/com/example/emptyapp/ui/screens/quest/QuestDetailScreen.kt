package com.example.emptyapp.ui.screens.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.ui.components.ChipVariant
import com.example.emptyapp.ui.components.MomentumCard
import com.example.emptyapp.ui.components.MomentumChip
import com.example.emptyapp.ui.components.MomentumPrimaryButton
import com.example.emptyapp.ui.components.MomentumQuietButton
import com.example.emptyapp.ui.components.MomentumSecondaryButton
import com.example.emptyapp.ui.theme.MomentumTheme

@Composable
fun QuestDetailScreen(
    questId: String,
    onBack: () -> Unit = {},
    onComplete: () -> Unit = {},
    onSaveForLater: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text("Quest", style = MaterialTheme.typography.titleLarge)
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MomentumChip("Academic", variant = ChipVariant.Category)
                    Text("3 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text("Read Chapter 4.", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Finish the assigned reading for History 101 before tomorrow's seminar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Open the chapter. Read in 10-minute blocks. Take one short break in the middle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MomentumPrimaryButton("Done for today", onComplete, Modifier.fillMaxWidth())
        MomentumSecondaryButton("Save for later", onSaveForLater, Modifier.fillMaxWidth())
        MomentumQuietButton("Not today", onSkip, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun QuestDetailPreview() {
    MomentumTheme { QuestDetailScreen(questId = "q1") }
}
