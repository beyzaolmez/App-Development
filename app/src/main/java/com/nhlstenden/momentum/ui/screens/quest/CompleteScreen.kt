package com.nhlstenden.momentum.ui.screens.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.data.StreakStore
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme

@Composable
fun CompleteScreen(
    onReflect: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val context = LocalContext.current

    // Record the completion once when this screen first appears.
    // LaunchedEffect(Unit) guarantees it runs exactly once per navigation to this screen.
    LaunchedEffect(Unit) {
        StreakStore.recordCompletion(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "Quest complete",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("That counts.", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Your streak was updated softly. You can stop here or write a quick note.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        MomentumPrimaryButton("Add a tiny reflection", onReflect, Modifier.fillMaxWidth())
        MomentumSecondaryButton("Return home", onHome, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun CompletePreview() {
    MomentumTheme { CompleteScreen() }
}
