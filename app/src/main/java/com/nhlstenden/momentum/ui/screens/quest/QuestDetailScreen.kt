package com.nhlstenden.momentum.ui.screens.quest

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
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun QuestDetailScreen(
    quest: Quest?,
    onBack: () -> Unit = {},
    onStart: () -> Unit = {},
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

        if (quest == null) {
            MissingQuestContent(onBack)
            return@Column
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MomentumChip(quest.category.label, variant = ChipVariant.Category)
                    MomentumChip(quest.status.label, variant = ChipVariant.Status)
                }
                Text(quest.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumChip("${quest.difficulty.label} · ${quest.estimatedMinutes} min", variant = ChipVariant.Neutral)
                    MomentumChip("+${quest.xp} XP", variant = ChipVariant.Reward)
                }
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleLarge)
                quest.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (quest.status) {
            QuestStatus.Available -> {
                MomentumPrimaryButton("Start quest", onStart, Modifier.fillMaxWidth())
                MomentumSecondaryButton("Save for later", onSaveForLater, Modifier.fillMaxWidth())
                MomentumQuietButton("Not today", onSkip, Modifier.fillMaxWidth())
            }
            QuestStatus.Active -> {
                MomentumPrimaryButton("Done for today", onComplete, Modifier.fillMaxWidth())
                MomentumQuietButton("Not today", onSkip, Modifier.fillMaxWidth())
            }
            QuestStatus.Completed -> {
                MomentumSecondaryButton("Return home", onBack, Modifier.fillMaxWidth())
            }
            QuestStatus.Skipped -> {
                MomentumPrimaryButton("Start again", onStart, Modifier.fillMaxWidth())
                MomentumSecondaryButton("Return home", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MissingQuestContent(onBack: () -> Unit) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quest not found", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This quest is no longer available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    MomentumPrimaryButton("Return home", onBack, Modifier.fillMaxWidth())
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun QuestDetailPreview() {
    val quest = PredefinedQuestRepository().getQuests().first()
    MomentumTheme { QuestDetailScreen(quest = quest) }
}
