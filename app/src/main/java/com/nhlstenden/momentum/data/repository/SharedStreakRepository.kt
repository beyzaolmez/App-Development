package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes shared streaks. A shared streak lives in the top-level
 * "sharedStreaks" collection and is keyed only by its member uids, so either
 * participant can load, update and observe the same document.
 */
interface SharedStreakRepository {
    /** All shared streaks (any status) the given user is a member of. */
    suspend fun getStreaksForUser(uid: String): List<SharedStreak>

    /** Observes all shared streaks the given user is a member of. */
    fun observeStreaksForUser(
        uid: String,
        onChange: (List<SharedStreak>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration

    /** Creates a Pending invitation from [inviterUid] to [inviteeUid]. Returns the new id. */
    suspend fun createInvitation(
        inviterUid: String,
        inviterName: String,
        inviteeUid: String,
        inviteeName: String
    ): String

    /** Marks a pending invitation as accepted (Active) or declined. */
    suspend fun respondToInvitation(streakId: String, accepted: Boolean)

    /** Cancels a pending invitation sent by the current user. */
    suspend fun cancelInvitation(streakId: String)

    /** Persists a streak that the logic layer has already updated. */
    suspend fun updateStreak(streak: SharedStreak)

    /**
     * Records that [uid] completed a quest today across every Active streak they
     * belong to, advancing each one when both members are done for the day.
     */
    suspend fun recordCompletion(uid: String, today: String = SharedStreakLogic.today())

    /** Mirrors a liked quest into each Active shared streak so friends can see it. */
    suspend fun recordQuestLike(uid: String, questId: String)

    /**
     * Checks if there is already an active or pending streak between uid1 and uid2.
     * Used to prevent duplicate invitations.
     */
    suspend fun hasExistingStreakWithUser(uid1: String, uid2: String): Boolean
}

class InMemorySharedStreakRepository : SharedStreakRepository {
    private val lock = Any()
    private val streaks = mutableMapOf<String, SharedStreak>()
    private var nextId = 1

    override suspend fun getStreaksForUser(uid: String): List<SharedStreak> =
        synchronized(lock) { streaks.values.filter { uid in it.memberIds } }

    override fun observeStreaksForUser(
        uid: String,
        onChange: (List<SharedStreak>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        onChange(synchronized(lock) { streaks.values.filter { uid in it.memberIds } })
        return ListenerRegistration {}
    }

    override suspend fun createInvitation(
        inviterUid: String,
        inviterName: String,
        inviteeUid: String,
        inviteeName: String
    ): String = synchronized(lock) {
        val id = "shared-${nextId++}"
        streaks[id] = SharedStreak(
            id = id,
            memberIds = listOf(inviterUid, inviteeUid),
            memberNames = mapOf(inviterUid to inviterName, inviteeUid to inviteeName),
            status = SharedStreakStatus.Pending,
            invitedByUid = inviterUid,
            createdAt = System.currentTimeMillis()
        )
        id
    }

    override suspend fun respondToInvitation(streakId: String, accepted: Boolean) = synchronized(lock) {
        val existing = streaks[streakId] ?: return@synchronized
        streaks[streakId] = if (accepted) {
            existing.copy(
                status = SharedStreakStatus.Active,
                currentStreak = 0,
                lastCompletionDates = emptyMap(),
                lastIncrementDate = null
            )
        } else {
            existing.copy(status = SharedStreakStatus.Declined)
        }
    }

    override suspend fun cancelInvitation(streakId: String) = synchronized(lock) {
        val existing = streaks[streakId] ?: return@synchronized
        if (existing.status == SharedStreakStatus.Pending) {
            streaks.remove(streakId)
        }
        Unit
    }

    override suspend fun updateStreak(streak: SharedStreak) = synchronized(lock) {
        streaks[streak.id] = streak
        Unit
    }

    override suspend fun recordCompletion(uid: String, today: String) = synchronized(lock) {
        streaks.values
            .filter { it.status == SharedStreakStatus.Active && uid in it.memberIds }
            .forEach { streak ->
                streaks[streak.id] = SharedStreakLogic.recordCompletion(streak, uid, today)
            }
    }

    override suspend fun recordQuestLike(uid: String, questId: String) = synchronized(lock) {
        streaks.values
            .filter { it.status == SharedStreakStatus.Active && uid in it.memberIds }
            .forEach { streak ->
                streaks[streak.id] = SharedStreakLogic.recordLike(streak, uid, questId)
            }
    }

    override suspend fun hasExistingStreakWithUser(uid1: String, uid2: String): Boolean =
        synchronized(lock) {
            streaks.values.any {
                uid1 in it.memberIds && uid2 in it.memberIds &&
                    (it.status == SharedStreakStatus.Active || it.status == SharedStreakStatus.Pending)
            }
        }
}

class FirestoreSharedStreakRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SharedStreakRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override suspend fun getStreaksForUser(uid: String): List<SharedStreak> {
        val snapshot = collection
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toSharedStreak() }
    }

    override fun observeStreaksForUser(
        uid: String,
        onChange: (List<SharedStreak>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        collection
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents?.mapNotNull { it.toSharedStreak() }.orEmpty())
            }

    override suspend fun createInvitation(
        inviterUid: String,
        inviterName: String,
        inviteeUid: String,
        inviteeName: String
    ): String {
        val document = collection.document()
        val streak = SharedStreak(
            id = document.id,
            memberIds = listOf(inviterUid, inviteeUid),
            memberNames = mapOf(inviterUid to inviterName, inviteeUid to inviteeName),
            status = SharedStreakStatus.Pending,
            invitedByUid = inviterUid,
            createdAt = System.currentTimeMillis()
        )
        document.set(streak.toFirestoreMap()).await()
        return document.id
    }

    override suspend fun respondToInvitation(streakId: String, accepted: Boolean) {
        val status = if (accepted) SharedStreakStatus.Active else SharedStreakStatus.Declined
        val updates = if (accepted) {
            mapOf(
                "status" to status.name,
                "currentStreak" to 0,
                "lastCompletionDates" to emptyMap<String, String>(),
                "lastIncrementDate" to null
            )
        } else {
            mapOf("status" to status.name)
        }
        collection.document(streakId).update(updates).await()
    }

    override suspend fun cancelInvitation(streakId: String) {
        collection.document(streakId).delete().await()
    }

    override suspend fun updateStreak(streak: SharedStreak) {
        collection.document(streak.id)
            .set(streak.toFirestoreMap())
            .await()
    }

    override suspend fun recordCompletion(uid: String, today: String) {
        // Use a transaction to prevent race conditions when both users complete
        // quests simultaneously. The transaction ensures atomic read-modify-write.
        val activeRefs = collection
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
            .documents
            .filter { it.getString("status") == SharedStreakStatus.Active.name }
            .map { it.reference }

        activeRefs.forEach { reference ->
            firestore.runTransaction { transaction ->
                val streak = transaction.get(reference).toSharedStreak()
                    ?: return@runTransaction
                if (streak.status != SharedStreakStatus.Active) return@runTransaction

                // First evaluate for today (checks if streak should be broken),
                // then record today's completion
                val evaluated = SharedStreakLogic.evaluateForToday(streak, today)
                val updated = SharedStreakLogic.recordCompletion(evaluated, uid, today)
                if (updated != streak) {
                    transaction.set(reference, updated.toFirestoreMap())
                }
            }.await()
        }
    }

    override suspend fun recordQuestLike(uid: String, questId: String) {
        val activeRefs = collection
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
            .documents
            .filter { it.getString("status") == SharedStreakStatus.Active.name }
            .map { it.reference }

        activeRefs.forEach { reference ->
            firestore.runTransaction { transaction ->
                val streak = transaction.get(reference).toSharedStreak()
                    ?: return@runTransaction
                val updated = SharedStreakLogic.recordLike(streak, uid, questId)
                if (updated != streak) {
                    transaction.set(reference, updated.toFirestoreMap())
                }
            }.await()
        }
    }

    override suspend fun hasExistingStreakWithUser(uid1: String, uid2: String): Boolean {
        // Query for any streak where both users are members and status is Active or Pending
        val snapshot = collection
            .whereArrayContains("memberIds", uid1)
            .get()
            .await()

        return snapshot.documents.any { doc ->
            val memberIds = (doc.get("memberIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val status = doc.getString("status")
            uid2 in memberIds && (status == SharedStreakStatus.Active.name || status == SharedStreakStatus.Pending.name)
        }
    }

    private companion object {
        const val COLLECTION = "sharedStreaks"
    }
}

private fun SharedStreak.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "memberIds" to memberIds,
    "memberNames" to memberNames,
    "status" to status.name,
    "invitedByUid" to invitedByUid,
    "currentStreak" to currentStreak,
    "lastCompletionDates" to lastCompletionDates,
    "likedQuestIdsByMember" to likedQuestIdsByMember,
    "lastIncrementDate" to lastIncrementDate,
    "createdAt" to createdAt
)

private fun com.google.firebase.firestore.DocumentSnapshot.toSharedStreak(): SharedStreak? {
    if (!exists()) return null

    val memberIds = (get("memberIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    val memberNames = (get("memberNames") as? Map<*, *>)
        ?.mapNotNull { (key, value) ->
            val k = key as? String ?: return@mapNotNull null
            val v = value as? String ?: return@mapNotNull null
            k to v
        }
        ?.toMap()
        .orEmpty()
    val lastCompletionDates = (get("lastCompletionDates") as? Map<*, *>)
        ?.mapNotNull { (key, value) ->
            val k = key as? String ?: return@mapNotNull null
            val v = value as? String ?: return@mapNotNull null
            k to v
        }
        ?.toMap()
        .orEmpty()
    val likedQuestIdsByMember = (get("likedQuestIdsByMember") as? Map<*, *>)
        ?.mapNotNull { (key, value) ->
            val k = key as? String ?: return@mapNotNull null
            val likedQuestIds = (value as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            k to likedQuestIds
        }
        ?.toMap()
        .orEmpty()
    val status = (getString("status"))
        ?.let { name -> SharedStreakStatus.values().firstOrNull { it.name == name } }
        ?: SharedStreakStatus.Pending

    return SharedStreak(
        id = id,
        memberIds = memberIds,
        memberNames = memberNames,
        status = status,
        invitedByUid = getString("invitedByUid").orEmpty(),
        currentStreak = (get("currentStreak") as? Number)?.toInt() ?: 0,
        lastCompletionDates = lastCompletionDates,
        likedQuestIdsByMember = likedQuestIdsByMember,
        lastIncrementDate = getString("lastIncrementDate"),
        createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L
    )
}
