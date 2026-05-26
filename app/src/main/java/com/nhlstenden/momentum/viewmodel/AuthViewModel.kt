package com.nhlstenden.momentum.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.nhlstenden.momentum.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, registrationError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, registrationError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, registrationError = null) }
    }

    fun register(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val nameError = validateName(currentState.name)
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)

        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, registrationError = null) }
            runCatching {
                authRepository.register(
                    name = currentState.name,
                    email = currentState.email.trim(),
                    password = currentState.password
                )
            }.onSuccess {
                _uiState.update { it.copy(isRegistering = false) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRegistering = false,
                        registrationError = error.toAuthMessage("Registration")
                    )
                }
            }
        }
    }

    fun signIn(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, registrationError = null) }
            runCatching {
                authRepository.signIn(currentState.email.trim(), currentState.password)
            }.onSuccess {
                _uiState.update { it.copy(isRegistering = false) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRegistering = false,
                        registrationError = error.toAuthMessage("Sign in")
                    )
                }
            }
        }
    }

    private fun validateName(name: String): String? =
        if (name.trim().isEmpty()) "Name is required." else null

    private fun validateEmail(email: String): String? = when {
        email.trim().isEmpty() -> "Email is required."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isEmpty() -> "Password is required."
        password.length < 6 -> "Password must be at least 6 characters."
        else -> null
    }
}

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val registrationError: String? = null,
    val isRegistering: Boolean = false
)

private fun Throwable.toAuthMessage(action: String): String {
    val firebaseCode = (this as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMessage = localizedMessage.orEmpty()
    val diagnosticText = "$firebaseCode $rawMessage".uppercase()

    return when {
        "CONFIGURATION_NOT_FOUND" in diagnosticText ->
            "Accounts are not enabled for this test build yet. Continue without an account for now."
        "EMAIL_ALREADY_IN_USE" in diagnosticText ->
            "This email already has an account. Try signing in instead."
        "INVALID_LOGIN_CREDENTIALS" in diagnosticText || "INVALID_CREDENTIAL" in diagnosticText ->
            "Email or password is incorrect."
        "NETWORK" in diagnosticText ->
            "Network error. Check your connection and try again."
        rawMessage.isNotBlank() -> rawMessage
        else -> "$action failed. Please try again."
    }
}
