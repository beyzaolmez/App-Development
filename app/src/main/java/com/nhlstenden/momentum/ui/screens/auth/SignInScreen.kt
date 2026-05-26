package com.nhlstenden.momentum.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val state by authViewModel.loginState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AuthHeader(title = "Welcome back", subtitle = "Continue your quests at your own pace.", onBack = onBack)

        MomentumTextField(
            value = state.email,
            onValueChange = authViewModel::onLoginEmailChanged,
            placeholder = "Student email",
            keyboardType = KeyboardType.Email,
            errorText = state.emailError
        )
        MomentumTextField(
            value = state.password,
            onValueChange = authViewModel::onLoginPasswordChanged,
            placeholder = "Password",
            isPassword = true,
            errorText = state.passwordError
        )

        val loginError = state.loginError
        if (loginError != null) {
            Text(loginError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        MomentumPrimaryButton(
            text = if (state.isLoggingIn) "Signing in…" else "Sign in",
            onClick = { authViewModel.login(onSignedIn) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoggingIn
        )
        MomentumQuietButton(
            text = "Forgot password",
            onClick = onForgot,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoggingIn
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun SignInScreenPreview() {
    MomentumTheme { SignInScreen() }
}
