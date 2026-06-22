package com.nhlstenden.momentum.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onContinueWithoutAccount: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            "Momentum",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Small steps count.",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Complete tiny daily quests across academic, social, wellbeing, focus, and movement goals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))

        MomentumPrimaryButton(
            text = "Sign in",
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        )
        MomentumSecondaryButton(
            text = "Create account",
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth()
        )
        MomentumQuietButton(
            text = "Continue without account",
            onClick = onContinueWithoutAccount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun WelcomeScreenPreview() {
    MomentumTheme { WelcomeScreen() }
}
