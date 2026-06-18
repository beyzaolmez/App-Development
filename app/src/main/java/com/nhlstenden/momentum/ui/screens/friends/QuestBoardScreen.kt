package com.nhlstenden.momentum.ui.screens.friends

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumInlineError
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.components.MomentumStatusCard
import com.nhlstenden.momentum.viewmodel.FriendLikedQuest
import com.nhlstenden.momentum.viewmodel.QuestBoardViewModel

@Composable
fun QuestBoardScreen(
    onBack: () -> Unit = {},
    viewModel: QuestBoardViewModel = viewModel()
) {
    QuestBoardContent(
        isSignedIn = viewModel.isSignedIn,
        isLoading = viewModel.isLoading,
        loadError = viewModel.loadError,
        friendLikedQuests = viewModel.friendLikedQuests,
        onRefresh = viewModel::refresh,
        onBack = onBack
    )
}

@Composable
private fun QuestBoardContent(
    isSignedIn: Boolean,
    isLoading: Boolean,
    loadError: String?,
    friendLikedQuests: List<FriendLikedQuest>,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Friends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Quest board", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Quests your friends have liked — discover what's working for the people around you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isSignedIn) {
            MomentumStatusCard(
                title = "Sign in required",
                message = "You need to be signed in to see what your friends are up to.",
                variant = ChipVariant.Neutral
            )
            MomentumQuietButton("Back", onBack, Modifier.fillMaxWidth())
            return@Column
        }

        if (loadError != null) {
            MomentumInlineError(loadError)
        }

        MomentumSecondaryButton(
            text = if (isLoading) "Loading..." else "Refresh",
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        if (!isLoading && loadError == null && friendLikedQuests.isEmpty()) {
            MomentumStatusCard(
                title = "Nothing here yet",
                message = "Once a friend likes a quest, it will appear here.",
                variant = ChipVariant.Neutral
            )
        }

        friendLikedQuests.forEach { item ->
            FriendLikedQuestCard(item)
        }

        MomentumQuietButton("Back", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FriendLikedQuestCard(item: FriendLikedQuest) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${item.friendName} liked this",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    item.quest.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                MomentumChip(item.quest.category.label, variant = ChipVariant.Category)
            }
            Text(
                item.quest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
