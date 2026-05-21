package com.nhlstenden.momentum.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun ProgressScreen(
    completedQuestCount: Int = 0,
    totalQuestCount: Int = 0,
    currentStreak: Int = 0
) {
    val questProgress = if (totalQuestCount == 0) {
        0f
    } else {
        completedQuestCount.toFloat() / totalQuestCount.toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Progress",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("This week", style = MaterialTheme.typography.headlineLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(currentStreak.toString(), "soft streak", Modifier.weight(1f))
            StatCard(completedQuestCount.toString(), "quests done", Modifier.weight(1f))
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quest progress", style = MaterialTheme.typography.titleLarge)
                ProgressRow(
                    label = "$completedQuestCount of $totalQuestCount quests completed",
                    fraction = questProgress,
                    color = MaterialTheme.colorScheme.primary
                )
                ProgressRow("Academic", 0.50f, MaterialTheme.colorScheme.primary)
                ProgressRow("Wellbeing", 0.25f, MaterialTheme.colorScheme.tertiary)
                ProgressRow("Social", 0.25f, MaterialTheme.colorScheme.secondary)
            }
        }
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
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun ProgressPreview() {
    MomentumTheme { ProgressScreen() }
}
