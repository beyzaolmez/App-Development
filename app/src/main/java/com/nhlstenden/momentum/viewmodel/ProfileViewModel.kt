package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var displayName by mutableStateOf(
        FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()
    )
        private set

    var isSaving by mutableStateOf(false)
        private set

    var saveError by mutableStateOf<String?>(null)
        private set

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            saveError = "Name cannot be empty."
            return
        }

        viewModelScope.launch {
            isSaving = true
            saveError = null
            runCatching {
                authRepository.updateDisplayName(trimmed)
            }.onSuccess {
                displayName = trimmed
                isSaving = false
            }.onFailure { error ->
                saveError = error.localizedMessage ?: "Failed to update name."
                isSaving = false
            }
        }
    }

    fun clearError() {
        saveError = null
    }
}
