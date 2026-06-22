package com.nhlstenden.momentum.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.nhlstenden.momentum.data.model.SharedStreakStatus
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
        val firebaseUser = result.user
            ?: throw IllegalStateException("Account creation returned no user")

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
        }.onFailure { Log.w(TAG, "register: failed to sync user profile; will retry on next sign-in", it) }
    }

    suspend fun ensureUserProfile(name: String? = null) {
        val firebaseUser = firebaseAuth.currentUser ?: return

        runCatching {
            withTimeout(5_000) {
                val existingUser = userRepository.getUser(firebaseUser.uid)
                if (existingUser != null) {
                    val displayName = existingUser.displayName.ifBlank {
                        name?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: firebaseUser.displayName.orEmpty()
                    }
                    val email = existingUser.email.ifBlank { firebaseUser.email.orEmpty() }
                    userRepository.saveUser(
                        existingUser.copy(
                            displayName = displayName,
                            email = email
                        )
                    )
                    return@withTimeout
                }

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
        }.onFailure { Log.w(TAG, "ensureUserProfile: profile sync failed", it) }
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
        }.onFailure { Log.w(TAG, "updateDisplayName: Firestore sync failed", it) }
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
     *  1. Reauthenticate, so Firebase Auth does not reject the final account
     *     deletion after Firestore data has already been removed.
     *  2. Removal of user-linked top-level data (shared streaks, feedback,
     *     quest suggestions).
     *  3. The user document and its private subcollections.
     *  4. The Firebase Auth user, last, so the account can no longer sign in.
     *
     * Returns Result.success(Unit) on success, Result.failure(exception) on error.
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(IllegalStateException("No user signed in"))
        val email = user.email ?: return Result.failure(IllegalStateException("No email found for signed-in user"))
        val uid = user.uid

        return runCatching {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()

            // 2. Cleanup top-level data linked to this user.
            deleteUserLinkedData(uid)

            // 3. Delete the user document and its private subcollections.
            userRepository.deleteUser(uid)

            // 4. Delete the Firebase Auth user last.
            user.delete().await()
        }
    }

    /**
     * Removes top-level Firestore documents that reference this user: feedback and
     * quest suggestions the user authored, plus ending any shared streaks.
     *
     * All writes are applied in a single atomic [com.google.firebase.firestore.WriteBatch]
     * so a partial failure cannot leave the user's data half-removed.
     *
     * Shared streaks are *ended* (status -> Declined) rather than deleted, because the
     * document is shared with another member whose history must not be destroyed.
     */
    private suspend fun deleteUserLinkedData(uid: String) {
        val batch = firestore.batch()

        // Shared streaks where the user is a member: end them non-destructively.
        firestore.collection("sharedStreaks")
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
            .documents
            .forEach { batch.update(it.reference, "status", SharedStreakStatus.Declined.name) }

        // Feedback authored by the user.
        firestore.collection("feedback")
            .whereEqualTo("uid", uid)
            .get()
            .await()
            .documents
            .forEach { batch.delete(it.reference) }

        // Quest suggestions authored by the user.
        firestore.collection("quest_suggestions")
            .whereEqualTo("uid", uid)
            .get()
            .await()
            .documents
            .forEach { batch.delete(it.reference) }

        batch.commit().await()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
