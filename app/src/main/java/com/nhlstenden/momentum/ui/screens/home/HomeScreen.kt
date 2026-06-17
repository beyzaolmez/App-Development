package com.nhlstenden.momentum.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumInlineError
import com.nhlstenden.momentum.ui.components.QuestCard
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.viewmodel.QuestDataMode
import com.nhlstenden.momentum.viewmodel.QuestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    quests: List<Quest>,
    isLoading: Boolean,
    greetingName: String,
    dataMode: QuestDataMode,
    errorMessage: String?,
    selectedStatus: QuestStatus?,
    activeQuestCount: Int,
    completedQuestCount: Int,
    dailyQuestLimit: Int,
    onStatusSelected: (QuestStatus?) -> Unit,
    onStartQuest: (String) -> Unit,
    onSkipQuest: (String) -> Unit,
    onQuestClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HomeHeader(greetingName) }
        item {
            Text(
                "Your side quests",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            Text(
                "$dailyQuestLimit daily quests · $activeQuestCount active · $completedQuestCount completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (errorMessage != null) {
            item { MomentumInlineError(errorMessage)
            }
        }
        item {
            QuestStatusFilters(
                selectedStatus = selectedStatus,
                onStatusSelected = onStatusSelected
            )
        }
        if (isLoading) {
            item {
                Text(
                    "Loading quests...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(quests, key = { it.id }) { quest ->
            QuestCard(
                title = quest.title,
                description = quest.description,
                category = quest.category.label,
                difficulty = if (quest.isLongTerm) {
                    "${quest.difficulty.label} · ${quest.targetProgress} ${quest.progressUnit}"
                } else {
                    "${quest.difficulty.label} · ${quest.estimatedMinutes} min"
                },
                status = quest.status.label,
                statusVariant = quest.status.chipVariant(),
                progressLabel = if (quest.isLongTerm) {
                    "${quest.currentProgress}/${quest.targetProgress} ${quest.progressUnit}"
                } else {
                    null
                },
                progressFraction = if (quest.isLongTerm) quest.progressFraction else null,
                actionLabel = if (quest.status == QuestStatus.Available) "Start quest" else null,
                onActionClick = { onStartQuest(quest.id) },
                secondaryActionLabel = if (quest.status == QuestStatus.Available) "Not today" else null,
                onSecondaryActionClick = { onSkipQuest(quest.id) },
                onClick = { onQuestClick(quest.id) }
            )
        }
    }
}

@Composable
private fun QuestStatusFilters(
    selectedStatus: QuestStatus?,
    onStatusSelected: (QuestStatus?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            MomentumChip(
                text = "All",
                variant = if (selectedStatus == null) ChipVariant.Skills else ChipVariant.Neutral,
                modifier = Modifier.clickable { onStatusSelected(null) }
            )
        }
        items(QuestStatus.entries.toList()) { status ->
            MomentumChip(
                text = status.label,
                variant = if (selectedStatus == status) ChipVariant.Skills else ChipVariant.Neutral,
                modifier = Modifier.clickable { onStatusSelected(status) }
            )
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
private fun HomeHeader(greetingName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            currentWeekday(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Hi $greetingName", style = MaterialTheme.typography.headlineLarge)
        MomentumChip(
            text = "Gentle day",
            variant = ChipVariant.Skills,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun HomeScreenPreview() {
    val questViewModel = QuestViewModel()
    MomentumTheme {
        HomeScreen(
            quests = questViewModel.visibleQuests(),
            isLoading = questViewModel.isLoading,
            greetingName = "Testing user",
            dataMode = questViewModel.dataMode,
            errorMessage = questViewModel.errorMessage,
            selectedStatus = questViewModel.selectedStatus,
            activeQuestCount = questViewModel.activeQuestCount(),
            completedQuestCount = questViewModel.completedQuestCount(),
            dailyQuestLimit = questViewModel.dailyQuestLimit(),
            onStatusSelected = questViewModel::selectStatus,
            onStartQuest = questViewModel::startQuest,
            onSkipQuest = questViewModel::skipQuest
        )
    }
}

private fun currentWeekday(): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
