package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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

    /** Creates a Pending invitation from [inviterUid] to [inviteeUid]. Returns the new id. */
    suspend fun createInvitation(
        inviterUid: String,
        inviterName: String,
        inviteeUid: String,
        inviteeName: String
    ): String

    /** Marks a pending invitation as accepted (Active) or declined. */
    suspend fun respondToInvitation(streakId: String, accepted: Boolean)

    /** Persists a streak that the logic layer has already updated. */
    suspend fun updateStreak(streak: SharedStreak)

    /**
     * Records that [uid] completed a quest today across every Active streak they
     * belong to, advancing each one when both members are done for the day.
     */
    suspend fun recordCompletion(uid: String, today: String = SharedStreakLogic.today())
}

class InMemorySharedStreakRepository : SharedStreakRepository {
    private val streaks = mutableMapOf<String, SharedStreak>()
    private var nextId = 1

    override suspend fun getStreaksForUser(uid: String): List<SharedStreak> =
        streaks.values.filter { uid in it.memberIds }

    override suspend fun createInvitation(
        inviterUid: String,
        inviterName: String,
        inviteeUid: String,
        inviteeName: String
    ): String {
        val id = "shared-${nextId++}"
        streaks[id] = SharedStreak(
            id = id,
            memberIds = listOf(inviterUid, inviteeUid),
            memberNames = mapOf(inviterUid to inviterName, inviteeUid to inviteeName),
            status = SharedStreakStatus.Pending,
            invitedByUid = inviterUid,
            createdAt = System.currentTimeMillis()
        )
        return id
    }

    override suspend fun respondToInvitation(streakId: String, accepted: Boolean) {
        val existing = streaks[streakId] ?: return
        streaks[streakId] = existing.copy(
            status = if (accepted) SharedStreakStatus.Active else SharedStreakStatus.Declined
        )
    }

    override suspend fun updateStreak(streak: SharedStreak) {
        streaks[streak.id] = streak
    }

    override suspend fun recordCompletion(uid: String, today: String) {
        streaks.values
            .filter { it.status == SharedStreakStatus.Active && uid in it.memberIds }
            .forEach { streak ->
                streaks[streak.id] = SharedStreakLogic.recordCompletion(streak, uid, today)
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
        collection.document(streakId)
            .update("status", status.name)
            .await()
    }

    override suspend fun updateStreak(streak: SharedStreak) {
        collection.document(streak.id)
            .set(streak.toFirestoreMap())
            .await()
    }

    override suspend fun recordCompletion(uid: String, today: String) {
        val active = collection
            .whereArrayContains("memberIds", uid)
            .get()
            .await()
            .documents
            .mapNotNull { it.toSharedStreak() }
            .filter { it.status == SharedStreakStatus.Active }

        active.forEach { streak ->
            val updated = SharedStreakLogic.recordCompletion(streak, uid, today)
            if (updated != streak) {
                updateStreak(updated)
            }
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
        lastIncrementDate = getString("lastIncrementDate"),
        createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L
    )
}