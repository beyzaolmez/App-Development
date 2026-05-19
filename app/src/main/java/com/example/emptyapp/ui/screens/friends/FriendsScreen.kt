package com.example.emptyapp.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.example.emptyapp.ui.components.MomentumSecondaryButton
import com.example.emptyapp.ui.theme.MomentumTheme

data class Friend(val name: String, val subtitle: String, val streak: Int)

private val sampleFriends = listOf(
    Friend("Noor", "Quiet reminders on", 3),
    Friend("Sam", "Invited", 0)
)

@Composable
fun FriendsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Friends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Shared streaks", style = MaterialTheme.typography.headlineLarge)

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shared with Noor", style = MaterialTheme.typography.titleLarge)
                    MomentumChip("3 days", variant = ChipVariant.Skills)
                }
                Text(
                    "Both of you completed one gentle quest this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        sampleFriends.forEach { friend ->
            FriendRow(friend)
        }

        MomentumSecondaryButton("Share invite code", {}, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FriendRow(friend: Friend) {
    MomentumCard {
        Row(
            modifier = it,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(friend.name, style = MaterialTheme.typography.titleLarge)
                Text(friend.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (friend.streak > 0) {
                MomentumChip("${friend.streak}", variant = ChipVariant.Category)
            } else {
                MomentumChip("new", variant = ChipVariant.Reward)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun FriendsPreview() {
    MomentumTheme { FriendsScreen() }
}
