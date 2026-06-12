package com.nhlstenden.momentum.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.model.CategoryProgressStat
import com.nhlstenden.momentum.data.model.ProgressOverview
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun ProgressScreen(overview: ProgressOverview = ProgressOverview()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProgressHeader() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = overview.currentStreak.toString(),
                    label = "quest streak",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = overview.totalCompletedQuests.toString(),
                    label = "quests done total",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { ActivityStatsCard(overview) }
    }
}

@Composable
private fun ProgressHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Progress",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Overview", style = MaterialTheme.typography.headlineLarge)
        Text(
            "A simple overview of your long-term quest progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    MomentumCard(modifier = modifier) {
        Column(it, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityStatsCard(overview: ProgressOverview) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activity statistics", style = MaterialTheme.typography.titleLarge)
                MomentumChip("All time", variant = ChipVariant.Status)
            }
            Text(
                "Preferred quest types",
                style = MaterialTheme.typography.titleMedium
            )
            if (overview.categoryStats.isEmpty()) {
                Text(
                    "Complete quests to see which activity types you choose most.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                overview.categoryStats.forEach { stat ->
                    ProgressRow(
                        label = "${stat.category.label}: ${stat.completedCount} completed",
                        fraction = stat.fraction,
                        color = stat.category.progressColor()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun QuestCategory.progressColor(): Color = when (this) {
    QuestCategory.Academic -> MaterialTheme.colorScheme.primary
    QuestCategory.Focus -> MaterialTheme.colorScheme.secondary
    QuestCategory.Wellbeing -> MaterialTheme.colorScheme.tertiary
    QuestCategory.Social -> MaterialTheme.colorScheme.secondary
    QuestCategory.Movement -> MaterialTheme.colorScheme.primary
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun ProgressPreview() {
    MomentumTheme {
        ProgressScreen(
            overview = ProgressOverview(
                completedToday = 2,
                totalDailyQuests = 3,
                totalCompletedQuests = 9,
                skippedQuestCount = 2,
                currentStreak = 4,
                categoryStats = listOf(
                    CategoryProgressStat(QuestCategory.Movement, completedCount = 4, fraction = 4f / 9f),
                    CategoryProgressStat(QuestCategory.Wellbeing, completedCount = 3, fraction = 3f / 9f),
                    CategoryProgressStat(QuestCategory.Social, completedCount = 2, fraction = 2f / 9f)
                )
            )
        )
    }
}
