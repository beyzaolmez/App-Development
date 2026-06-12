package com.nhlstenden.momentum.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.momentum.data.repository.AuthRepository
import com.nhlstenden.momentum.util.toFriendlyPasswordResetMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, requestError = null) }
    }

    fun sendResetLink(onSent: () -> Unit) {
        val email = _uiState.value.email.trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, requestError = null) }
            runCatching {
                authRepository.sendPasswordResetEmail(email)
            }.onSuccess {
                _uiState.update { it.copy(isSending = false, isEmailSent = true) }
                onSent()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        requestError = error.toFriendlyPasswordResetMessage()
                    )
                }
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val requestError: String? = null,
    val isSending: Boolean = false,
    val isEmailSent: Boolean = false
)
