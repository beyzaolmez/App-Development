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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.util.HapticHelper

@Composable
fun CompleteScreen(
    hasReflection: Boolean = false,
    onReflect: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val view = LocalView.current
    LaunchedEffect(Unit) {
        HapticHelper.questComplete(view)
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
            if (hasReflection) {
                "Your reflection was saved and your streak was updated softly."
            } else {
                "Your streak was updated softly. You can stop here or write a quick note."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        if (hasReflection) {
            MomentumPrimaryButton("Return home", onHome, Modifier.fillMaxWidth())
        } else {
            MomentumPrimaryButton("Add a tiny reflection", onReflect, Modifier.fillMaxWidth())
            MomentumSecondaryButton("Return home", onHome, Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun CompletePreview() {
    MomentumTheme { CompleteScreen() }
}
