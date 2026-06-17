package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nhlstenden.momentum.data.model.Friendship
import com.nhlstenden.momentum.data.model.FriendshipStatus
import com.nhlstenden.momentum.data.model.FriendUser
import kotlinx.coroutines.tasks.await
import java.util.Locale

interface FriendRepository {
    /**
     * Search for users by display name or email.
     * Returns users that are not already friends and have no pending requests.
     */
    suspend fun searchUsers(query: String, currentUid: String): List<FriendUser>

    /**
     * Send a friend request from requesterUid to recipientUid.
     */
    suspend fun sendFriendRequest(requesterUid: String, requesterName: String, recipientUid: String, recipientName: String): String

    /**
     * Accept a pending friend request.
     */
    suspend fun acceptFriendRequest(friendshipId: String)

    /**
     * Decline or cancel a friend request.
     */
    suspend fun declineFriendRequest(friendshipId: String)

    /**
     * Remove an existing friendship.
     */
    suspend fun removeFriend(friendshipId: String)

    /**
     * Observe all friendships for a user (both sent and received).
     */
    fun observeFriendships(
        uid: String,
        onChange: (List<Friendship>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration

    /**
     * Check if two users are already friends.
     */
    suspend fun areFriends(uid1: String, uid2: String): Boolean
}

class InMemoryFriendRepository : FriendRepository {
    private val friendships = mutableMapOf<String, Friendship>()

    override suspend fun searchUsers(query: String, currentUid: String): List<FriendUser> = emptyList()

    override suspend fun sendFriendRequest(
        requesterUid: String,
        requesterName: String,
        recipientUid: String,
        recipientName: String
    ): String {
        val id = "friendship_${System.currentTimeMillis()}"
        friendships[id] = Friendship(
            id = id,
            requesterUid = requesterUid,
            requesterName = requesterName,
            recipientUid = recipientUid,
            recipientName = recipientName,
            status = FriendshipStatus.Pending,
            createdAt = System.currentTimeMillis()
        )
        return id
    }

    override suspend fun acceptFriendRequest(friendshipId: String) {
        friendships[friendshipId] = friendships[friendshipId]?.copy(
            status = FriendshipStatus.Accepted,
            acceptedAt = System.currentTimeMillis()
        ) ?: return
    }

    override suspend fun declineFriendRequest(friendshipId: String) {
        friendships[friendshipId] = friendships[friendshipId]?.copy(
            status = FriendshipStatus.Declined
        ) ?: return
    }

    override suspend fun removeFriend(friendshipId: String) {
        friendships.remove(friendshipId)
    }

    override fun observeFriendships(
        uid: String,
        onChange: (List<Friendship>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        val userFriendships = friendships.values.filter {
            it.requesterUid == uid || it.recipientUid == uid
        }
        onChange(userFriendships)
        return ListenerRegistration {}
    }

    override suspend fun areFriends(uid1: String, uid2: String): Boolean {
        return friendships.values.any {
            (it.requesterUid == uid1 && it.recipientUid == uid2 ||
             it.requesterUid == uid2 && it.recipientUid == uid1) &&
            it.status == FriendshipStatus.Accepted
        }
    }
}

class FirestoreFriendRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendRepository {
    private val collection get() = firestore.collection("friendships")

    override suspend fun searchUsers(query: String, currentUid: String): List<FriendUser> {
        val normalizedQuery = query.trim().lowercase(Locale.US)
        if (normalizedQuery.isEmpty()) return emptyList()

        // Only read friendships the current user is part of. Reading the whole
        // collection would be denied by the security rules, which restrict reads
        // to the requester/recipient of each friendship.
        val sentSnapshot = collection
            .whereEqualTo("requesterUid", currentUid)
            .get()
            .await()
        val receivedSnapshot = collection
            .whereEqualTo("recipientUid", currentUid)
            .get()
            .await()

        val existingFriendIds = (sentSnapshot.documents + receivedSnapshot.documents)
            .mapNotNull { it.toFriendship() }
            .map {
                if (it.requesterUid == currentUid) it.recipientUid else it.requesterUid
            }
            .toSet()

        // Search by display name
        val nameSnapshot = firestore.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query.trim())
            .whereLessThanOrEqualTo("displayName", query.trim() + "\uf8ff")
            .get()
            .await()

        // Search by email
        val emailSnapshot = firestore.collection("users")
            .whereEqualTo("emailLowercase", normalizedQuery)
            .get()
            .await()

        // Combine and filter results
        val results = mutableMapOf<String, FriendUser>()

        (nameSnapshot.documents + emailSnapshot.documents).forEach { doc ->
            val uid = doc.id
            if (uid != currentUid && uid !in existingFriendIds) {
                results[uid] = FriendUser(
                    uid = uid,
                    displayName = doc.getString("displayName").orEmpty(),
                    email = doc.getString("email").orEmpty()
                )
            }
        }

        return results.values.toList()
    }

    override suspend fun sendFriendRequest(
        requesterUid: String,
        requesterName: String,
        recipientUid: String,
        recipientName: String
    ): String {
        val document = collection.document()
        val friendship = Friendship(
            id = document.id,
            requesterUid = requesterUid,
            requesterName = requesterName,
            recipientUid = recipientUid,
            recipientName = recipientName,
            status = FriendshipStatus.Pending,
            createdAt = System.currentTimeMillis()
        )
        document.set(friendship.toFirestoreMap()).await()
        return document.id
    }

    override suspend fun acceptFriendRequest(friendshipId: String) {
        collection.document(friendshipId)
            .update(
                "status", FriendshipStatus.Accepted.name,
                "acceptedAt", System.currentTimeMillis()
            )
            .await()
    }

    override suspend fun declineFriendRequest(friendshipId: String) {
        collection.document(friendshipId)
            .update("status", FriendshipStatus.Declined.name)
            .await()
    }

    override suspend fun removeFriend(friendshipId: String) {
        collection.document(friendshipId).delete().await()
    }

    override fun observeFriendships(
        uid: String,
        onChange: (List<Friendship>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        // We need to observe friendships where user is either requester or recipient
        // Firestore doesn't support OR queries, so we use two listeners and merge
        val friendships = mutableMapOf<String, Friendship>()

        val requesterListener = collection
            .whereEqualTo("requesterUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    doc.toFriendship()?.let { friendships[it.id] = it }
                }
                // Also need recipient listener results, handled below
            }

        val recipientListener = collection
            .whereEqualTo("recipientUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    doc.toFriendship()?.let { friendships[it.id] = it }
                }
                onChange(friendships.values.toList())
            }

        // Return a composite listener that removes both
        return ListenerRegistration {
            requesterListener.remove()
            recipientListener.remove()
        }
    }

    override suspend fun areFriends(uid1: String, uid2: String): Boolean {
        val snapshot = collection
            .whereEqualTo("requesterUid", uid1)
            .whereEqualTo("recipientUid", uid2)
            .get()
            .await()

        if (snapshot.documents.any { it.getString("status") == FriendshipStatus.Accepted.name }) {
            return true
        }

        val reverseSnapshot = collection
            .whereEqualTo("requesterUid", uid2)
            .whereEqualTo("recipientUid", uid1)
            .get()
            .await()

        return reverseSnapshot.documents.any { it.getString("status") == FriendshipStatus.Accepted.name }
    }
}

private fun Friendship.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "requesterUid" to requesterUid,
    "requesterName" to requesterName,
    "recipientUid" to recipientUid,
    "recipientName" to recipientName,
    "status" to status.name,
    "createdAt" to createdAt,
    "acceptedAt" to acceptedAt
)

private fun com.google.firebase.firestore.DocumentSnapshot.toFriendship(): Friendship? {
    val id = getString("id") ?: return null
    return Friendship(
        id = id,
        requesterUid = getString("requesterUid").orEmpty(),
        requesterName = getString("requesterName").orEmpty(),
        recipientUid = getString("recipientUid").orEmpty(),
        recipientName = getString("recipientName").orEmpty(),
        status = getString("status").toFriendshipStatus(),
        createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L,
        acceptedAt = (get("acceptedAt") as? Number)?.toLong()
    )
}

private fun String?.toFriendshipStatus(): FriendshipStatus =
    enumValues<FriendshipStatus>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: FriendshipStatus.Pending
