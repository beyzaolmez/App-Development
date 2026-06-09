package com.example.emptyapp.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.data.SuggestionsStore
import com.example.emptyapp.ui.components.MomentumPrimaryButton
import com.example.emptyapp.ui.components.MomentumQuietButton
import com.example.emptyapp.ui.components.MomentumTextField
import com.example.emptyapp.ui.theme.MomentumTheme

@Composable
fun SuggestQuestScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        // Confirmation state shown after successful submission
        SubmittedContent(onBack = onBack)
    } else {
        FormContent(
            title = title,
            onTitleChange = { title = it },
            category = category,
            onCategoryChange = { category = it },
            notes = notes,
            onNotesChange = { notes = it },
            onSubmit = {
                SuggestionsStore.save(context, title, category, notes)
                submitted = true
            },
            onBack = onBack
        )
    }
}

@Composable
private fun FormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    // G5-130: Submit is only enabled when the title is not blank.
    val isValid = title.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Suggest a quest",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Got an idea for a tiny daily quest? Share it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Title — required field
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Quest title *",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MomentumTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "e.g. Try a new walking route"
            )
        }

        // Category — optional
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Category (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MomentumTextField(
                value = category,
                onValueChange = onCategoryChange,
                placeholder = "e.g. Outdoors, Social, Focus…"
            )
        }

        // Notes — optional, multi-line
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Notes (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Any extra details…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 5,
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
        }

        Spacer(Modifier.weight(1f))

        MomentumPrimaryButton(
            text = "Submit suggestion",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = isValid
        )
        MomentumQuietButton(
            text = "Cancel",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SubmittedContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Thanks for sharing!",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Your quest idea has been saved. We review all suggestions when shaping new quests.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        MomentumPrimaryButton(
            text = "Back to profile",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun SuggestQuestPreview() {
    MomentumTheme { SuggestQuestScreen() }
}
