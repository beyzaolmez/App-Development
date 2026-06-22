package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import kotlinx.coroutines.tasks.await
import java.util.Locale

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun findByEmail(email: String): User?
    fun observeUser(uid: String, onChange: (User?) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration
    suspend fun saveUser(user: User)
    suspend fun updateDisplayName(uid: String, displayName: String)
    suspend fun updateInterests(uid: String, interests: List<String>)
    suspend fun updateNotificationPreference(uid: String, enabled: Boolean)
    suspend fun updateProgress(uid: String, progress: UserProgress)
    suspend fun updateThemePreference(uid: String, themePreference: String)

    /**
     * Atomically reads the user's current [UserProgress], applies [transform], and
     * persists the result, returning the value that was written. Implementations
     * must guarantee the read-modify-write is not subject to lost updates when
     * called concurrently (e.g. completing two quests in quick succession).
     */
    suspend fun applyProgressUpdate(
        uid: String,
        transform: (UserProgress) -> UserProgress
    ): UserProgress

    /**
     * Deletes the user document and all subcollections from Firestore.
     * This should be called before deleting the Firebase Auth account.
     */
    suspend fun deleteUser(uid: String)
}

class InMemoryUserRepository : UserRepository {
    private val lock = Any()
    private val users = mutableMapOf<String, User>()

    override suspend fun getUser(uid: String): User? = synchronized(lock) { users[uid] }

    override suspend fun findByEmail(email: String): User? =
        users.values.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

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

    override suspend fun applyProgressUpdate(
        uid: String,
        transform: (UserProgress) -> UserProgress
    ): UserProgress = synchronized(lock) {
        val current = users[uid]?.progress ?: UserProgress()
        val updated = transform(current)
        users[uid] = users[uid]?.copy(progress = updated)
            ?: User(uid = uid, displayName = "", email = "", progress = updated)
        updated
    }

    override suspend fun updateThemePreference(uid: String, themePreference: String) {
        users[uid] = users[uid]?.copy(themePreference = themePreference) ?: return
    }

    override suspend fun deleteUser(uid: String) {
        users.remove(uid)
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
            ?: return null
        return document.toUser(document.id)
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

    override suspend fun updateThemePreference(uid: String, themePreference: String) {
        firestore.collection("users")
            .document(uid)
            .update("themePreference", themePreference)
            .await()
    }

    override suspend fun applyProgressUpdate(
        uid: String,
        transform: (UserProgress) -> UserProgress
    ): UserProgress {
        val reference = firestore.collection("users").document(uid)
        return firestore.runTransaction { transaction ->
            val current = (transaction.get(reference).get("progress") as? Map<*, *>).toUserProgress()
            val updated = transform(current)
            transaction.set(reference, mapOf("progress" to updated.toFirestoreMap()), SetOptions.merge())
            updated
        }.await()
    }

    override suspend fun deleteUser(uid: String) {
        val userDoc = firestore.collection("users").document(uid)

        // Collect every subcollection document, then the user document itself.
        val subcollections = listOf("questStates", "questFeedback", "journalEntries")
        val references = buildList {
            subcollections.forEach { subcollection ->
                userDoc.collection(subcollection).get().await().documents
                    .forEach { add(it.reference) }
            }
            add(userDoc)
        }

        // Commit in chunks so a user with many states/entries cannot exceed
        // Firestore's 500-operations-per-batch limit. All deletes are idempotent,
        // so a retry after a transient failure is safe.
        references.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private companion object {
        // Firestore allows at most 500 writes per batch; stay safely under it.
        const val BATCH_LIMIT = 450
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
    "onboardingCompleted" to onboardingCompleted,
    "themePreference" to themePreference
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
        onboardingCompleted = getBoolean("onboardingCompleted") ?: false,
        themePreference = getString("themePreference")
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
