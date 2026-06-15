package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun saveUser(user: User)
    suspend fun updateDisplayName(uid: String, displayName: String)
    suspend fun updateInterests(uid: String, interests: List<String>)
    suspend fun updateNotificationPreference(uid: String, enabled: Boolean)
    suspend fun updateProgress(uid: String, progress: UserProgress)
}

class InMemoryUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()

    override suspend fun getUser(uid: String): User? = users[uid]

    override suspend fun findByEmail(email: String): User? =
        users.values.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun saveUser(user: User) {
        users[user.uid] = user
    }

    override suspend fun updateDisplayName(uid: String, displayName: String) {
        users[uid] = users[uid]?.copy(displayName = displayName) ?: return
    }

    override suspend fun updateInterests(uid: String, interests: List<String>) {
        users[uid] = users[uid]?.copy(interests = interests) ?: return
    }

    override suspend fun updateNotificationPreference(uid: String, enabled: Boolean) {
        users[uid] = users[uid]?.copy(notificationEnabled = enabled) ?: return
    }

    override suspend fun updateProgress(uid: String, progress: UserProgress) {
        users[uid] = users[uid]?.copy(progress = progress) ?: return
    }
}

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {
    override suspend fun getUser(uid: String): User? {
        val document = firestore.collection("users").document(uid).get().await()
        if (!document.exists()) return null

        val progressMap = document.get("progress") as? Map<*, *>
        return User(
            uid = uid,
            displayName = document.getString("displayName").orEmpty(),
            email = document.getString("email").orEmpty(),
            interests = document.get("interests").toStringList(),
            notificationEnabled = document.getBoolean("notificationEnabled") ?: false,
            progress = progressMap.toUserProgress(),
            onboardingCompleted = document.getBoolean("onboardingCompleted") ?: false
        )
    }

    override suspend fun findByEmail(email: String): User? {
        val normalized = email.trim()
        if (normalized.isEmpty()) return null

        val snapshot = firestore.collection("users")
            .whereEqualTo("email", normalized)
            .limit(1)
            .get()
            .await()

        val document = snapshot.documents.firstOrNull() ?: return null
        val progressMap = document.get("progress") as? Map<*, *>
        return User(
            uid = document.id,
            displayName = document.getString("displayName").orEmpty(),
            email = document.getString("email").orEmpty(),
            interests = document.get("interests").toStringList(),
            notificationEnabled = document.getBoolean("notificationEnabled") ?: false,
            progress = progressMap.toUserProgress(),
            onboardingCompleted = document.getBoolean("onboardingCompleted") ?: false
        )
    }

    override suspend fun saveUser(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user.toFirestoreMap())
            .await()
    }

    override suspend fun updateDisplayName(uid: String, displayName: String) {
        firestore.collection("users")
            .document(uid)
            .update("displayName", displayName)
            .await()
    }

    override suspend fun updateInterests(uid: String, interests: List<String>) {
        firestore.collection("users")
            .document(uid)
            .update("interests", interests)
            .await()
    }

    override suspend fun updateNotificationPreference(uid: String, enabled: Boolean) {
        firestore.collection("users")
            .document(uid)
            .update("notificationEnabled", enabled)
            .await()
    }

    override suspend fun updateProgress(uid: String, progress: UserProgress) {
        firestore.collection("users")
            .document(uid)
            .update("progress", progress.toFirestoreMap())
            .await()
    }
}

private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "displayName" to displayName,
    "email" to email,
    "interests" to interests,
    "notificationEnabled" to notificationEnabled,
    "progress" to progress.toFirestoreMap(),
    "onboardingCompleted" to onboardingCompleted
)

private fun UserProgress.toFirestoreMap(): Map<String, Any?> = mapOf(
    "currentStreak" to currentStreak,
    "completedQuestCount" to completedQuestCount,
    "skippedQuestCount" to skippedQuestCount,
    "categoryCounts" to categoryCounts,
    "lastQuestCompletionDate" to lastQuestCompletionDate
)

private fun Map<*, *>?.toUserProgress(): UserProgress {
    if (this == null) return UserProgress()

    return UserProgress(
        currentStreak = (this["currentStreak"] as? Number)?.toInt() ?: 0,
        completedQuestCount = (this["completedQuestCount"] as? Number)?.toInt() ?: 0,
        skippedQuestCount = (this["skippedQuestCount"] as? Number)?.toInt() ?: 0,
        categoryCounts = (this["categoryCounts"] as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val category = key as? String ?: return@mapNotNull null
                val count = (value as? Number)?.toInt() ?: return@mapNotNull null
                category to count
            }
            ?.toMap()
            .orEmpty(),
        lastQuestCompletionDate = this["lastQuestCompletionDate"] as? String
    )
}

private fun Any?.toStringList(): List<String> =
    (this as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()
