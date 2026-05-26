package com.nhlstenden.momentum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun register(name: String, email: String, password: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()
        result.user?.updateProfile(profileUpdates)?.await()
    }

    suspend fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
}
