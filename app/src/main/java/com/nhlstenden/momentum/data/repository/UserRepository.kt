package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import kotlinx.coroutines.tasks.await
import java.util.Locale

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun findByEmail(email: String): User?
    /**
     * Search users by email prefix. Returns users whose email starts with the query
     * (case-insensitive), excluding the current user's own account.
     */
    suspend fun searchByEmail(query: String, excludeUid: String): List<User>
    fun observeUser(uid: String, onChange: (User?) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration
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

    override suspend fun searchByEmail(query: String, excludeUid: String): List<User> {
        val normalizedQuery = query.trim().lowercase(Locale.US)
        if (normalizedQuery.isEmpty()) return emptyList()
        return users.values.filter {
            it.uid != excludeUid &&
            it.email.lowercase(Locale.US).startsWith(normalizedQuery)
        }
    }

    override fun observeUser(
        uid: String,
        onChange: (User?) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        onChange(users[uid])
        return ListenerRegistration {}
    }

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
        return document.toUser(uid)
    }

    override suspend fun findByEmail(email: String): User? {
        val normalized = email.trim()
        if (normalized.isEmpty()) return null
        val normalizedLower = normalized.lowercase(Locale.US)

        val snapshot = firestore.collection("users")
            .whereEqualTo("emailLowercase", normalizedLower)
            .limit(1)
            .get()
            .await()

        val document = snapshot.documents.firstOrNull()
            ?: firestore.collection("users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
            ?: firestore.collection("users")
                .get()
                .await()
                .documents
                .firstOrNull { it.getString("email").orEmpty().equals(normalized, ignoreCase = true) }
            ?: return null
        return document.toUser(document.id)
    }

    override suspend fun searchByEmail(query: String, excludeUid: String): List<User> {
        val normalizedQuery = query.trim().lowercase(Locale.US)
        if (normalizedQuery.isEmpty()) return emptyList()

        // Use prefix matching (>= query and <= query + \uf8ff) on emailLowercase.
        // This finds emails that start with the query (e.g., "ale" matches "alex@example.com").
        // The \uf8ff suffix is the highest Unicode code point, creating an upper bound.
        val endRange = normalizedQuery + "\uf8ff"

        val snapshot = firestore.collection("users")
            .whereGreaterThanOrEqualTo("emailLowercase", normalizedQuery)
            .whereLessThanOrEqualTo("emailLowercase", endRange)
            .limit(20)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc -> doc.toUser(doc.id) }
            .filter { it.uid != excludeUid }
    }

    override fun observeUser(
        uid: String,
        onChange: (User?) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.toUser(uid))
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
    "emailLowercase" to email.trim().lowercase(Locale.US),
    "interests" to interests,
    "notificationEnabled" to notificationEnabled,
    "progress" to progress.toFirestoreMap(),
    "onboardingCompleted" to onboardingCompleted
)

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(uid: String): User? {
    if (!exists()) return null

    val progressMap = get("progress") as? Map<*, *>
    return User(
        uid = uid,
        displayName = getString("displayName").orEmpty(),
        email = getString("email").orEmpty(),
        interests = get("interests").toStringList(),
        notificationEnabled = getBoolean("notificationEnabled") ?: false,
        progress = progressMap.toUserProgress(),
        onboardingCompleted = getBoolean("onboardingCompleted") ?: false
    )
}

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
