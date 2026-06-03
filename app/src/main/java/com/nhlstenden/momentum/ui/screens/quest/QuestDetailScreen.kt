package com.nhlstenden.momentum.ui.screens.quest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nhlstenden.momentum.ui.theme.PillShape

@Composable
fun QuestDetailScreen(
    quest: Quest?,
    onBack: () -> Unit = {},
    onStart: () -> Unit = {},
    onComplete: () -> Unit = {},
    onSaveForLater: () -> Unit = {},
    onSkip: () -> Unit = {},
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    onLike: () -> Unit = {},
    onDislike: () -> Unit = {},
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
            return@Column
        }

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
                    MomentumChip("${quest.difficulty.label} · ${quest.estimatedMinutes} min", variant = ChipVariant.Neutral)
                }
                QuestReactionButtons(
                    isLiked = isLiked,
                    isDisliked = isDisliked,
                    onLike = onLike,
                    onDislike = onDislike
                )
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

        QuestFeedbackCard(
            selectedFeedback = selectedFeedback,
            onFeedbackSelected = onFeedbackSelected
        )

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
private fun QuestReactionButtons(
    isLiked: Boolean,
    isDisliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ReactionButton(
            text = "Like",
            icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            selected = isLiked,
            onClick = onLike,
            modifier = Modifier.weight(1f)
        )
        ReactionButton(
            text = "Dislike",
            icon = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            selected = isDisliked,
            onClick = onDislike,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReactionButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = text)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = PillShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) { content() }
    }
}

@Composable
private fun QuestFeedbackCard(
    selectedFeedback: QuestFeedbackType?,
    onFeedbackSelected: (QuestFeedbackType) -> Unit
) {
    val feedbackOptions = QuestFeedbackType.entries

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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(feedbackOptions) { option ->
                    MomentumChip(
                        text = option.label,
                        variant = if (selectedFeedback == option) ChipVariant.Skills else ChipVariant.Neutral,
                        modifier = Modifier.clickable { onFeedbackSelected(option) }
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
