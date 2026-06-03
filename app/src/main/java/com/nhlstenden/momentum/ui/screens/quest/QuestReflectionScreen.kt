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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun QuestReflectionScreen(
    quest: Quest?,
    prompt: String,
    errorText: String? = null,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    onSave: (note: String, promptChoice: String, quickTake: String?) -> Boolean = { _, _, _ -> true }
) {
    val promptOptions = rememberPromptOptions(prompt)
    val quickTakeOptions = listOf("Worth it", "Okay", "More like this", "Not for me")
    var selectedPrompt by rememberSaveable(quest?.id) { mutableStateOf(promptOptions.first()) }
    var selectedQuickTake by rememberSaveable(quest?.id) { mutableStateOf<String?>(null) }
    var note by rememberSaveable(quest?.id) { mutableStateOf("") }

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
            Text("Quest Reflection", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, "Close", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (quest == null) {
            MomentumCard {
                Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quest not found", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "This reflection can no longer be connected to a quest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MomentumQuietButton("Return home", onClose, Modifier.fillMaxWidth())
            return@Column
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MomentumChip("Related quest", variant = ChipVariant.Skills)
                    Text(
                        "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MomentumChip("Quest reflection", variant = ChipVariant.Reward)
                Text(prompt, style = MaterialTheme.typography.titleLarge)
                promptOptions.forEach { option ->
                    MomentumChip(
                        text = option,
                        variant = if (selectedPrompt == option) ChipVariant.Reward else ChipVariant.Neutral,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrompt = option }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text("One short reflection...", style = MaterialTheme.typography.bodyMedium)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    isError = errorText != null,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (errorText != null) {
                    Text(
                        errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick take", style = MaterialTheme.typography.titleLarge)
                quickTakeOptions.chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowOptions.forEach { option ->
                            MomentumChip(
                                text = option,
                                variant = if (selectedQuickTake == option) ChipVariant.Skills else ChipVariant.Neutral,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedQuickTake = option }
                            )
                        }
                    }
                }
            }
        }

        MomentumPrimaryButton(
            "Save reflection",
            { onSave(note, selectedPrompt, selectedQuickTake) },
            Modifier.fillMaxWidth()
        )
        MomentumQuietButton("Skip", onClose, Modifier.fillMaxWidth())
    }
}

private fun rememberPromptOptions(prompt: String): List<String> = listOf(
    prompt,
    "Something I could do better next time",
    "Would try something else next time"
)

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 760)
@Composable
private fun QuestReflectionPreview() {
    val quest = PredefinedQuestRepository().getQuests().first()
    MomentumTheme {
        QuestReflectionScreen(
            quest = quest,
            prompt = "What did you notice, learn, or want to do differently next time?"
        )
    }
}
