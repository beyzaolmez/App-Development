package com.nhlstenden.momentum.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun onLoginEmailChanged(email: String) {
        _loginState.update { it.copy(email = email, emailError = null, loginError = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _loginState.update { it.copy(password = password, passwordError = null, loginError = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _loginState.value
        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.isEmpty()) "Password is required." else null

        if (emailError != null || passwordError != null) {
            _loginState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoggingIn = true, loginError = null) }
            runCatching {
                authRepository.login(state.email.trim(), state.password)
            }.onSuccess {
                _loginState.update { it.copy(isLoggingIn = false) }
                onSuccess()
            }.onFailure { error ->
                val message = when {
                    error.message?.contains("no user record", ignoreCase = true) == true -> "No account found with this email."
                    error.message?.contains("password is invalid", ignoreCase = true) == true -> "Incorrect password."
                    error.message?.contains("badly formatted", ignoreCase = true) == true -> "Enter a valid email address."
                    else -> error.localizedMessage ?: "Sign in failed. Please try again."
                }
                _loginState.update { it.copy(isLoggingIn = false, loginError = message) }
            }
        }
    }

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
                authRepository.register(currentState.name.trim(), currentState.email.trim(), currentState.password)
            }.onSuccess {
                _uiState.update { it.copy(isRegistering = false) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRegistering = false,
                        registrationError = error.localizedMessage ?: "Registration failed. Please try again."
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
    val isLoggingIn: Boolean = false
)
