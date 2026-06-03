package com.nhlstenden.momentum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.nhlstenden.momentum.data.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = FirestoreUserRepository()
) {
    val currentUser get() = firebaseAuth.currentUser

    suspend fun register(name: String, email: String, password: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: return

        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
        ).await()

        runCatching {
            withTimeout(5_000) {
                userRepository.saveUser(
                    User(
                        uid = firebaseUser.uid,
                        displayName = name.trim(),
                        email = email.trim()
                    )
                )
            }
        }
    }

    suspend fun ensureUserProfile(name: String? = null) {
        val firebaseUser = firebaseAuth.currentUser ?: return

        runCatching {
            withTimeout(5_000) {
                userRepository.saveUser(
                    User(
                        uid = firebaseUser.uid,
                        displayName = name?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: firebaseUser.displayName.orEmpty(),
                        email = firebaseUser.email.orEmpty()
                    )
                )
            }
        }
    }

    suspend fun updateDisplayName(name: String) {
        val firebaseUser = firebaseAuth.currentUser ?: return
        val trimmed = name.trim()

        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(trimmed)
                .build()
        ).await()

        runCatching {
            withTimeout(5_000) {
                userRepository.updateDisplayName(firebaseUser.uid, trimmed)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        ensureUserProfile()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
