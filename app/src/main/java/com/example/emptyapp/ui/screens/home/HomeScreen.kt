package com.example.emptyapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.notification.NotificationHelper
import com.example.emptyapp.ui.components.MomentumChip
import com.example.emptyapp.ui.components.ChipVariant
import com.example.emptyapp.ui.components.QuestCard
import com.example.emptyapp.ui.theme.MomentumTheme

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val xp: Int,
    val difficulty: String
)

// Brandbook + minimal-UI acceptance criteria: max 1–3 active quests on Home.
private val sampleQuests = listOf(
    Quest("q1", "Read Chapter 4", "Finish the History 101 reading before tomorrow's seminar.", "Academic", 150, "Medium"),
    Quest("q2", "15-minute mindful reset", "A short grounding quest between classes.", "Personal", 80, "Easy"),
    Quest("q3", "Message one friend", "Send one low-pressure check-in to someone you trust.", "Social", 60, "Easy")
)

@Composable
fun HomeScreen(
    quests: List<Quest> = sampleQuests,
    onQuestClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HomeHeader() }
        item {
            Text(
                "Your side quests",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        items(quests, key = { it.id }) { quest ->
            QuestCard(
                title = quest.title,
                description = quest.description,
                category = quest.category,
                xp = quest.xp,
                difficulty = quest.difficulty,
                onClick = { onQuestClick(quest.id) }
            )
        }
        item {
            Button(
                onClick = {
                    NotificationHelper.showQuestNotification(
                        context = context,
                        questTitle = quests.firstOrNull()?.title ?: "Your quest awaits"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test notification")
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Thursday",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Hi Miriam", style = MaterialTheme.typography.headlineLarge)
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
    MomentumTheme { HomeScreen() }
}
