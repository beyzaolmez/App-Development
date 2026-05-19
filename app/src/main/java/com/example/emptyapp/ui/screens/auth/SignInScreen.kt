package com.example.emptyapp.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emptyapp.ui.components.MomentumPrimaryButton
import com.example.emptyapp.ui.components.MomentumQuietButton
import com.example.emptyapp.ui.components.MomentumTextField
import com.example.emptyapp.ui.theme.MomentumTheme

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit = {},
    onForgot: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AuthHeader(title = "Welcome back", subtitle = "Continue your quests at your own pace.", onBack = onBack)

        MomentumTextField(email, { email = it }, "Student email", keyboardType = KeyboardType.Email)
        MomentumTextField(password, { password = it }, "Password", isPassword = true)

        MomentumPrimaryButton("Sign in", onSignedIn, Modifier.fillMaxWidth())
        MomentumQuietButton("Forgot password", onForgot, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun SignInScreenPreview() {
    MomentumTheme { SignInScreen() }
}
