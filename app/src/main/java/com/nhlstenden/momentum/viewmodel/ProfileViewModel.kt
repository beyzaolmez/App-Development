package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.nhlstenden.momentum.data.repository.AuthRepository
import com.nhlstenden.momentum.util.FriendlyErrorMessages
import com.nhlstenden.momentum.util.toFriendlyAccountDeletionMessage
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    var displayName by mutableStateOf(
        firebaseAuth.currentUser?.displayName.orEmpty()
    )
        private set

    // Keep displayName in sync with the current Firebase user so the value does
    // not persist after sign out or when switching to a different account.
    private val authStateListener = AuthStateListener { auth ->
        displayName = auth.currentUser?.displayName.orEmpty()
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    var isSaving by mutableStateOf(false)
        private set

    var saveError by mutableStateOf<String?>(null)
        private set

    var isDeleting by mutableStateOf(false)
        private set

    var deleteError by mutableStateOf<String?>(null)
        private set

    var deleteSuccess by mutableStateOf(false)
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
            }.onFailure {
                saveError = FriendlyErrorMessages.profileSave()
                isSaving = false
            }
        }
    }

    fun clearError() {
        saveError = null
    }

    /**
     * Deletes the current user's account. This action is irreversible.
     * On success, [deleteSuccess] will be true and the UI should navigate to login.
     * On failure, [deleteError] will contain an error message.
     */
    fun deleteAccount(password: String) {
        if (password.isBlank()) {
            deleteError = "Enter your password to delete your account."
            return
        }

        viewModelScope.launch {
            isDeleting = true
            deleteError = null
            deleteSuccess = false

            authRepository.deleteAccount(password)
                .onSuccess {
                    deleteSuccess = true
                    isDeleting = false
                }
                .onFailure { error ->
                    deleteError = error.toFriendlyAccountDeletionMessage()
                    isDeleting = false
                }
        }
    }

    fun clearDeleteError() {
        deleteError = null
    }

    fun resetDeleteState() {
        deleteSuccess = false
        deleteError = null
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}
