package com.nhlstenden.momentum.ui.screens.quest

import androidx.compose.foundation.clickable
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
import com.nhlstenden.momentum.data.model.QuestFeedbackType
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
    onUpdateProgress: () -> Unit = {},
    canUpdateProgress: Boolean = true,
    onSaveForLater: () -> Unit = {},
    onSkip: () -> Unit = {},
    selectedFeedback: QuestFeedbackType? = null,
    onFeedbackSelected: (QuestFeedbackType) -> Unit = {}
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
        } else {
            MomentumCard {
                Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MomentumChip(quest.category.label, variant = ChipVariant.Category)
                        MomentumChip(quest.status.label, variant = quest.status.chipVariant())
                    }
                    Text(quest.title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MomentumChip(
                            if (quest.isLongTerm) {
                                "${quest.difficulty.label} · ${quest.targetProgress} ${quest.progressUnit}"
                            } else {
                                "${quest.difficulty.label} · ${quest.estimatedMinutes} min"
                            },
                            variant = ChipVariant.Neutral
                        )
                    }
                }
            }

            if (quest.isLongTerm) {
                LongTermProgressCard(quest = quest)
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

            QuestFeedbackCard(
                selectedFeedback = selectedFeedback,
                onFeedbackSelected = onFeedbackSelected
            )

            if (quest.isLongTerm) {
                LongTermQuestActions(
                    quest = quest,
                    onBack = onBack,
                    onStart = onStart,
                    onUpdateProgress = onUpdateProgress,
                    canUpdateProgress = canUpdateProgress,
                    onSaveForLater = onSaveForLater,
                    onSkip = onSkip
                )
            } else {
                DailyQuestActions(
                    quest = quest,
                    onBack = onBack,
                    onStart = onStart,
                    onComplete = onComplete,
                    onSaveForLater = onSaveForLater,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun LongTermQuestActions(
    quest: Quest,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onUpdateProgress: () -> Unit,
    canUpdateProgress: Boolean,
    onSaveForLater: () -> Unit,
    onSkip: () -> Unit
) {
    when (quest.status) {
        QuestStatus.Available -> {
            MomentumPrimaryButton("Start quest", onStart, Modifier.fillMaxWidth())
            MomentumSecondaryButton("Save for later", onSaveForLater, Modifier.fillMaxWidth())
            MomentumQuietButton("Not today", onSkip, Modifier.fillMaxWidth())
        }
        QuestStatus.Active -> {
            MomentumPrimaryButton(
                text = if (canUpdateProgress) "Log 1 ${quest.progressUnit}" else "Progress logged today",
                onClick = onUpdateProgress,
                modifier = Modifier.fillMaxWidth(),
                enabled = canUpdateProgress
            )
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

@Composable
private fun DailyQuestActions(
    quest: Quest,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onSaveForLater: () -> Unit,
    onSkip: () -> Unit
) {
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

@Composable
private fun LongTermProgressCard(quest: Quest) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Long-term progress", style = MaterialTheme.typography.titleLarge)
            Text(
                "${quest.currentProgress}/${quest.targetProgress} ${quest.progressUnit}",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                if (quest.status == QuestStatus.Completed) "Goal reached." else "Update this as you make progress across days or weeks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuestFeedbackCard(
    selectedFeedback: QuestFeedbackType?,
    onFeedbackSelected: (QuestFeedbackType) -> Unit
) {
    val feedbackOptions = listOf(QuestFeedbackType.Like, QuestFeedbackType.Dislike)

    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Quest feedback", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Optional",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                "Helps tune future quest categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                feedbackOptions.forEach { option ->
                    MomentumChip(
                        text = option.label,
                        variant = if (selectedFeedback == option) ChipVariant.Skills else ChipVariant.Neutral,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFeedbackSelected(option) }
                    )
                }
            }
        }
    }
}

private fun QuestStatus.chipVariant(): ChipVariant = when (this) {
    QuestStatus.Available -> ChipVariant.Category
    QuestStatus.Active -> ChipVariant.Status
    QuestStatus.Completed -> ChipVariant.Reward
    QuestStatus.Skipped -> ChipVariant.Neutral
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
