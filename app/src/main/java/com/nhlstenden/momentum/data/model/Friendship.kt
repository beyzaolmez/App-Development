package com.nhlstenden.momentum.data.model

/**
 * Represents a friendship between two users.
 * This is separate from shared streaks - it's a social connection.
 */
data class Friendship(
    val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val recipientUid: String = "",
    val recipientName: String = "",
    val status: FriendshipStatus = FriendshipStatus.Pending,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null
)

enum class FriendshipStatus {
    Pending,    // Friend request sent, waiting for response
    Accepted,   // Friends!
    Declined,   // Request declined
    Blocked     // One user blocked the other
}

/**
 * A user that can be found in search and added as a friend.
 */
data class FriendUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val isFriend: Boolean = false,
    val hasPendingRequest: Boolean = false
)
