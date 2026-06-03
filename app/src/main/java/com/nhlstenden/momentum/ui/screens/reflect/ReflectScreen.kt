package com.nhlstenden.momentum.ui.screens.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumStatusCard
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun ReflectScreen(onSave: () -> Unit = {}) {
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Reflection", style = MaterialTheme.typography.headlineLarge)
        MomentumStatusCard(
            title = "Sprint 2 placeholder",
            message = "Reflection notes are not saved yet. This screen only previews the planned quest reflection flow.",
            variant = ChipVariant.Reward
        )

        MomentumCard {
            Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MomentumChip("Optional check-in", variant = ChipVariant.Reward)
                Text("How did that feel?", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("One short note (optional)…", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth(),
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
                Text(
                    "Skip this if writing feels like too much.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        MomentumPrimaryButton("Preview save", onSave, Modifier.fillMaxWidth(), enabled = false)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun ReflectPreview() {
    MomentumTheme { ReflectScreen() }
}
