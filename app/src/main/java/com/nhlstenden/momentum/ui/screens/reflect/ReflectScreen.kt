package com.nhlstenden.momentum.ui.screens.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.model.JournalEntry
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.util.MomentumDateFormat

@Composable
fun ReflectScreen(
    reflections: List<JournalEntry> = emptyList(),
    questTitleForReflection: (String) -> String = { "Quest reflection" }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Reflect",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Reflection history", style = MaterialTheme.typography.headlineLarge)

        if (reflections.isEmpty()) {
            EmptyReflectionHistory()
        } else {
            reflections.forEach { entry ->
                ReflectionHistoryCard(
                    entry = entry,
                    questTitle = questTitleForReflection(entry.questId)
                )
            }
        }
    }
}

@Composable
private fun EmptyReflectionHistory() {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MomentumChip("Private journal", variant = ChipVariant.Reward)
            Text("No reflections yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Complete a quest and save a reflection to build your history here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReflectionHistoryCard(entry: JournalEntry, questTitle: String) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MomentumChip("Private", variant = ChipVariant.Reward)
                Text(
                    entry.createdAt.toHistoryDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                questTitle,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            entry.quickTake?.takeIf { it.isNotBlank() }?.let { quickTake ->
                MomentumChip(quickTake, variant = ChipVariant.Skills)
            }
            entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.promptChoice?.takeIf { it.isNotBlank() }?.let { prompt ->
                Text(
                    prompt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun Long.toHistoryDate(): String =
    MomentumDateFormat.formatHistory(this)

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun ReflectPreview() {
    MomentumTheme {
        ReflectScreen(
            reflections = listOf(
                JournalEntry(
                    journalEntryId = "walk-loop",
                    questId = "walk-loop",
                    promptChoice = "What did you notice during the walk?",
                    quickTake = "Good",
                    note = "It helped clear my head between classes.",
                    createdAt = System.currentTimeMillis()
                )
            ),
            questTitleForReflection = { "Take a campus walk" }
        )
    }
}
