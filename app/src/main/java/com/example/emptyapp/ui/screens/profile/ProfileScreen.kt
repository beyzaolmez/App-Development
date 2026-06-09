package com.example.emptyapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.ui.components.ChipVariant
import com.example.emptyapp.ui.components.MomentumCard
import com.example.emptyapp.ui.components.MomentumChip
import com.example.emptyapp.ui.components.MomentumQuietButton
import com.example.emptyapp.ui.components.MomentumSecondaryButton
import com.example.emptyapp.ui.theme.MomentumTheme

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    onSuggestQuest: () -> Unit = {},
    onFeedback: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineLarge)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Miriam", style = MaterialTheme.typography.titleLarge)
                    Text("University student", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MomentumSecondaryButton("Edit", {})
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingRow("Notifications", "One quiet reminder")
                SettingRow("Private journal", "Only visible to you")
                SettingRow("Shared streaks", "Friends you choose")
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Interests", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumChip("Mind reset", variant = ChipVariant.Category)
                    MomentumChip("Social ease", variant = ChipVariant.Skills)
                    MomentumChip("Reflection", variant = ChipVariant.Reward)
                }
            }
        }

        MomentumSecondaryButton("Send feedback", onFeedback, Modifier.fillMaxWidth())
        MomentumSecondaryButton("Suggest a quest", onSuggestQuest, Modifier.fillMaxWidth())
        MomentumQuietButton("Sign out", onSignOut, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    var on by remember { mutableStateOf(true) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = on,
            onCheckedChange = { on = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun ProfilePreview() {
    MomentumTheme { ProfileScreen() }
}
