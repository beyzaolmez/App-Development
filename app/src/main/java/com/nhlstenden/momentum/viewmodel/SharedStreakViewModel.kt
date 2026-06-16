package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.model.FriendStreak
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.data.repository.FirestoreSharedStreakRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.data.repository.SharedStreakRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Drives the Friends / shared-streak screen: loading the current user's shared
 * streaks, inviting a friend by email, and accepting or declining invitations.
 */
class SharedStreakViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val sharedStreakRepository: SharedStreakRepository = FirestoreSharedStreakRepository()
) : ViewModel() {

    /** Accepted streaks the user is actively maintaining (already re-evaluated for today). */
    var activeStreaks by mutableStateOf<List<SharedStreak>>(emptyList())
        private set

    /** Invitations sent to me that I can accept or decline. */
    var incomingInvitations by mutableStateOf<List<SharedStreak>>(emptyList())
        private set

    /** Invitations I sent that are still waiting for the other person. */
    var outgoingInvitations by mutableStateOf<List<SharedStreak>>(emptyList())
        private set

    /**
     * Each connected friend's own individual streak, so the user can see how their
     * friends are doing. A "connected friend" is the other member of an Active shared
     * streak; their streak comes from their own user document.
     */
    var friendStreaks by mutableStateOf<List<FriendStreak>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var inviteInProgress by mutableStateOf(false)
        private set

    var inviteError by mutableStateOf<String?>(null)
        private set

    var inviteSuccess by mutableStateOf<String?>(null)
        private set

    /** False when running without an account — shared streaks need a signed-in user. */
    val isSignedIn: Boolean get() = auth.currentUser != null

    init {
        load()
    }

    fun load() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            activeStreaks = emptyList()
            incomingInvitations = emptyList()
            outgoingInvitations = emptyList()
            friendStreaks = emptyList()
            return
        }

        viewModelScope.launch {
            isLoading = true
            loadError = null
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.getStreaksForUser(uid)
                }
            }.onSuccess { streaks ->
                applyStreaks(uid, streaks)
            }.onFailure {
                loadError = "We couldn't load your shared streaks. Pull to refresh or try again later."
            }
            isLoading = false
        }
    }

    fun invite(email: String) {
        val inviter = auth.currentUser
        val inviterUid = inviter?.uid
        if (inviterUid == null) {
            inviteError = "Sign in to start a shared streak with a friend."
            return
        }

        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            inviteError = "Enter your friend's email address."
            return
        }
        if (normalizedEmail.equals(inviter.email, ignoreCase = true)) {
            inviteError = "You can't start a shared streak with yourself."
            return
        }

        viewModelScope.launch {
            inviteInProgress = true
            inviteError = null
            inviteSuccess = null

            val friend = runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) { userRepository.findByEmail(normalizedEmail) }
            }.getOrNull()

            if (friend == null) {
                inviteError = "No Momentum user found with that email. Ask your friend to sign up first."
                inviteInProgress = false
                return@launch
            }

            val alreadyConnected = (activeStreaks + incomingInvitations + outgoingInvitations)
                .any { friend.uid in it.memberIds }
            if (alreadyConnected) {
                inviteError = "You already have a shared streak or pending invite with ${friend.displayName.ifBlank { "this friend" }}."
                inviteInProgress = false
                return@launch
            }

            val inviterName = inviter.displayName?.takeIf { it.isNotBlank() }
                ?: inviter.email?.substringBefore("@")
                ?: "A friend"

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.createInvitation(
                        inviterUid = inviterUid,
                        inviterName = inviterName,
                        inviteeUid = friend.uid,
                        inviteeName = friend.displayName.ifBlank { friend.email.substringBefore("@") }
                    )
                }
            }.onSuccess {
                inviteSuccess = "Invitation sent to ${friend.displayName.ifBlank { normalizedEmail }}."
                load()
            }.onFailure {
                inviteError = "We couldn't send the invitation. Please try again."
            }
            inviteInProgress = false
        }
    }

    fun accept(streakId: String) = respond(streakId, accepted = true)

    fun decline(streakId: String) = respond(streakId, accepted = false)

    fun clearInviteFeedback() {
        inviteError = null
        inviteSuccess = null
    }

    private fun respond(streakId: String, accepted: Boolean) {
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.respondToInvitation(streakId, accepted)
                }
            }.onSuccess {
                load()
            }.onFailure {
                loadError = "We couldn't update that invitation. Please try again."
            }
        }
    }

    /**
     * Splits loaded streaks into the three UI buckets and re-evaluates active ones for
     * today. When evaluating breaks a streak (count reset to 0), the reset is persisted
     * so both members see the broken state consistently.
     */
    private fun applyStreaks(uid: String, streaks: List<SharedStreak>) {
        val today = SharedStreakLogic.today()
        val evaluatedActive = mutableListOf<SharedStreak>()
        val incoming = mutableListOf<SharedStreak>()
        val outgoing = mutableListOf<SharedStreak>()

        streaks.forEach { streak ->
            when (streak.status) {
                SharedStreakStatus.Active -> {
                    val evaluated = SharedStreakLogic.evaluateForToday(streak, today)
                    if (evaluated != streak) persist(evaluated)
                    evaluatedActive += evaluated
                }
                SharedStreakStatus.Pending ->
                    if (streak.invitedByUid == uid) outgoing += streak else incoming += streak
                SharedStreakStatus.Declined -> Unit
            }
        }

        activeStreaks = evaluatedActive.sortedByDescending { it.currentStreak }
        incomingInvitations = incoming
        outgoingInvitations = outgoing

        // Connected friends are the other members of active streaks; load each one's
        // own personal streak so the user can see how their friends are doing.
        val friendIds = evaluatedActive.mapNotNull { it.otherMemberId(uid) }.distinct()
        loadFriendStreaks(friendIds)
    }

    /** Fetches each connected friend's individual streak from their user document. */
    private fun loadFriendStreaks(friendIds: List<String>) {
        if (friendIds.isEmpty()) {
            friendStreaks = emptyList()
            return
        }

        viewModelScope.launch {
            val loaded = friendIds.mapNotNull { friendId ->
                val friend = runCatching {
                    withTimeout(FIRESTORE_TIMEOUT_MS) { userRepository.getUser(friendId) }
                }.getOrNull() ?: return@mapNotNull null

                FriendStreak(
                    uid = friend.uid,
                    displayName = friend.displayName.ifBlank { friend.email.substringBefore("@") },
                    currentStreak = friend.progress.currentStreak,
                    lastQuestCompletionDate = friend.progress.lastQuestCompletionDate
                )
            }
            friendStreaks = loaded.sortedByDescending { it.currentStreak }
        }
    }

    private fun persist(streak: SharedStreak) {
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) { sharedStreakRepository.updateStreak(streak) }
            }
        }
    }

    private companion object {
        const val FIRESTORE_TIMEOUT_MS = 8_000L
    }
}