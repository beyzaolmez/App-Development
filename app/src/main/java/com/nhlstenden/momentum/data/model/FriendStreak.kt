package com.nhlstenden.momentum.data.model

/**
 * A connected friend's own individual streak, shown so the user can see how each
 * friend is doing and stay socially motivated.
 *
 * This is distinct from [SharedStreak]: a shared streak is a single joint counter
 * between two people, whereas a [FriendStreak] is the friend's *personal* quest
 * streak read from their own user document ([UserProgress.currentStreak]). It
 * therefore reflects the friend's latest synced progress.
 */
data class FriendStreak(
    val uid: String,
    val displayName: String,
    val currentStreak: Int,
    val lastQuestCompletionDate: String? = null
)