package com.nhlstenden.momentum.ui.screens.profile

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.FeedbackStore
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun FeedbackScreen(onBack: () -> Unit = {}) {
    var message by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    if (submitted) {
        FeedbackSubmittedContent(onBack = onBack)
    } else {
        FeedbackFormContent(
            message = message,
            onMessageChange = { message = it },
            submitting = submitting,
            onSubmit = {
                submitting = true
                FeedbackStore.save(
                    message = message,
                    onSuccess = { submitted = true },
                    onError = { submitted = true }
                )
            },
            onBack = onBack
        )
    }
}

@Composable
private fun FeedbackFormContent(
    message: String,
    onMessageChange: (String) -> Unit,
    submitting: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val isValid = message.isNotBlank() && !submitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Send feedback",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Help us improve Momentum. What's on your mind?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Your feedback *",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Tell us what you think, what's working well, or what could be better…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 5,
                maxLines = 8,
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
            text = if (submitting) "Submitting…" else "Submit feedback",
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
private fun FeedbackSubmittedContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Thanks for your feedback!",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Your input helps us make Momentum better for everyone.",
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
private fun FeedbackPreview() {
    MomentumTheme { FeedbackScreen() }
}
