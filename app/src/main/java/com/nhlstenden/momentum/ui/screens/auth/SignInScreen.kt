package com.nhlstenden.momentum.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhlstenden.momentum.ui.components.MomentumInlineError
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.components.MomentumTextField
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit = {},
    onForgot: () -> Unit = {},
    onBack: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AuthHeader(title = "Welcome back", subtitle = "Continue your quests at your own pace.", onBack = onBack)

        MomentumTextField(
            value = uiState.email,
            onValueChange = authViewModel::onEmailChanged,
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
            errorText = uiState.emailError
        )
        MomentumTextField(
            value = uiState.password,
            onValueChange = authViewModel::onPasswordChanged,
            placeholder = "Password",
            isPassword = true,
            errorText = uiState.passwordError
        )

        val signInError = uiState.registrationError
        if (signInError != null) {
            MomentumInlineError(signInError)
        }

        MomentumPrimaryButton(
            text = if (uiState.isRegistering) "Signing in..." else "Sign in",
            onClick = { authViewModel.signIn(onSignedIn) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isRegistering
        )
        MomentumQuietButton(
            text = "Forgot password",
            onClick = onForgot,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isRegistering
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun SignInScreenPreview() {
    MomentumTheme { SignInScreen() }
}
