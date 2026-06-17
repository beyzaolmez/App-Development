package com.nhlstenden.momentum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.nhlstenden.momentum.data.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
                val existingUser = userRepository.getUser(firebaseUser.uid)
                if (existingUser != null) return@withTimeout

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

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Deletes the current user's account from Firebase Auth and Firestore.
     *
     * Cleanup order (all while still authenticated, since the rules require it):
     *  1. Best-effort removal of user-linked top-level data (shared streaks,
     *     feedback, quest suggestions). These are best-effort so a hiccup here
     *     never blocks the critical account removal below.
     *  2. The user document and its private subcollections.
     *  3. The Firebase Auth user, last, so the account can no longer sign in.
     *
     * Returns Result.success(Unit) on success, Result.failure(exception) on error.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(IllegalStateException("No user signed in"))
        val uid = user.uid

        return runCatching {
            // 1. Best-effort cleanup of top-level data linked to this user.
            runCatching { deleteUserLinkedData(uid) }

            // 2. Delete the user document and its private subcollections.
            userRepository.deleteUser(uid)

            // 3. Delete the Firebase Auth user last.
            user.delete().await()
        }
    }

    /**
     * Removes top-level Firestore documents that reference this user: shared
     * streaks the user is a member of, and feedback / quest suggestions the
     * user authored.
     */
    private suspend fun deleteUserLinkedData(uid: String) {
        // Shared streaks where the user is a member (either participant).
        val streaks = firestore.collection("sharedStreaks")
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
        streaks.documents.forEach { it.reference.delete().await() }

        // Feedback authored by the user.
        val feedback = firestore.collection("feedback")
            .whereEqualTo("uid", uid)
            .get()
            .await()
        feedback.documents.forEach { it.reference.delete().await() }

        // Quest suggestions authored by the user.
        val suggestions = firestore.collection("quest_suggestions")
            .whereEqualTo("uid", uid)
            .get()
            .await()
        suggestions.documents.forEach { it.reference.delete().await() }
    }
}
