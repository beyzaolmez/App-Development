package com.nhlstenden.momentum.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.nhlstenden.momentum.data.model.FriendStreak
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.data.repository.FirestoreSharedStreakRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.data.repository.SharedStreakRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import com.nhlstenden.momentum.data.model.User
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

    /** Invitations I sent that were declined by the other person. */
    var declinedInvitations by mutableStateOf<List<SharedStreak>>(emptyList())
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

    private var streakListener: ListenerRegistration? = null
    private val friendListeners = mutableMapOf<String, ListenerRegistration>()
    private val friendStreaksById = mutableMapOf<String, FriendStreak>()

    init {
        observeStreaks()
    }

    fun refresh() {
        observeStreaks()
    }

    fun load() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            activeStreaks = emptyList()
            incomingInvitations = emptyList()
            outgoingInvitations = emptyList()
            declinedInvitations = emptyList()
            friendStreaks = emptyList()
            isLoading = false
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

    private fun observeStreaks() {
        val uid = auth.currentUser?.uid
        streakListener?.remove()
        streakListener = null
        clearFriendListeners()

        if (uid == null) {
            activeStreaks = emptyList()
            incomingInvitations = emptyList()
            outgoingInvitations = emptyList()
            friendStreaks = emptyList()
            return
        }

        ensureCurrentUserProfile()
        isLoading = true
        loadError = null
        streakListener = sharedStreakRepository.observeStreaksForUser(
            uid = uid,
            onChange = { streaks ->
                isLoading = false
                loadError = null
                applyStreaks(uid, streaks)
            },
            onError = {
                isLoading = false
                loadError = "We couldn't keep your shared streaks in sync. Try refreshing."
            }
        )
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

            ensureCurrentUserProfile()

            val friendResult = runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) { userRepository.findByEmail(normalizedEmail) }
            }

            if (friendResult.isFailure) {
                inviteError = "We couldn't search for that friend right now. Please try again."
                inviteInProgress = false
                return@launch
            }

            val friend = friendResult.getOrNull()
            if (friend == null) {
                inviteError = "No Momentum profile found with that email. Ask your friend to sign in once, then try again."
                inviteInProgress = false
                return@launch
            }

            // Atomic check via Firestore to prevent race conditions when two devices
            // try to invite the same person simultaneously
            val alreadyConnected = runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.hasExistingStreakWithUser(inviterUid, friend.uid)
                }
            }.getOrDefault(false)

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
                refresh()
            }.onFailure {
                inviteError = "We couldn't send the invitation. Please try again."
            }
            inviteInProgress = false
        }
    }

    fun accept(streakId: String) {
        val invitation = incomingInvitations.firstOrNull { it.id == streakId }
        if (invitation == null) {
            respond(streakId, accepted = true)
            return
        }

        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.updateStreak(activateWithTodayProgress(invitation))
                }
            }.onSuccess {
                refresh()
            }.onFailure {
                loadError = "We couldn't accept that invitation. Please try again."
            }
        }
    }

    fun decline(streakId: String) = respond(streakId, accepted = false)

    fun cancelInvitation(streakId: String) {
        val uid = auth.currentUser?.uid
        val invitation = outgoingInvitations.firstOrNull { it.id == streakId }
        if (uid == null || invitation?.invitedByUid != uid) {
            loadError = "We couldn't cancel that invitation. Please refresh and try again."
            return
        }

        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.cancelInvitation(streakId)
                }
            }.onSuccess {
                refresh()
            }.onFailure {
                loadError = "We couldn't cancel that invitation. Please try again."
            }
        }
    }

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
                refresh()
            }.onFailure {
                loadError = "We couldn't update that invitation. Please try again."
            }
        }
    }

    private suspend fun activateWithTodayProgress(streak: SharedStreak): SharedStreak {
        val today = SharedStreakLogic.today()
        val completionDates = streak.memberIds
            .mapNotNull { uid ->
                val user = userRepository.getUser(uid) ?: return@mapNotNull null
                if (user.progress.lastQuestCompletionDate == today) uid to today else null
            }
            .toMap()

        val activated = streak.copy(
            status = SharedStreakStatus.Active,
            currentStreak = 0,
            lastCompletionDates = completionDates,
            lastIncrementDate = null
        )

        return if (activated.bothCompletedOn(today)) {
            SharedStreakLogic.recordCompletion(activated, activated.memberIds.first(), today)
        } else {
            activated
        }
    }

    /**
     * Splits loaded streaks into the UI buckets and re-evaluates active ones for
     * today. When evaluating breaks a streak (count reset to 0), the reset is persisted
     * so both members see the broken state consistently.
     */
    private fun applyStreaks(uid: String, streaks: List<SharedStreak>) {
        val today = SharedStreakLogic.today()
        val evaluatedActive = mutableListOf<SharedStreak>()
        val incoming = mutableListOf<SharedStreak>()
        val outgoing = mutableListOf<SharedStreak>()
        val declined = mutableListOf<SharedStreak>()

        streaks.forEach { streak ->
            when (streak.status) {
                SharedStreakStatus.Active -> {
                    val evaluated = SharedStreakLogic.evaluateForToday(streak, today)
                    if (evaluated != streak) persist(evaluated)
                    evaluatedActive += evaluated
                }
                SharedStreakStatus.Pending ->
                    if (streak.invitedByUid == uid) outgoing += streak else incoming += streak
                SharedStreakStatus.Declined ->
                    if (streak.invitedByUid == uid) declined += streak
            }
        }

        activeStreaks = evaluatedActive.sortedByDescending { it.currentStreak }
        incomingInvitations = incoming
        outgoingInvitations = outgoing
        declinedInvitations = declined

        // Connected friends are the other members of active streaks; observe each one's
        // own personal streak so the user can see live progress changes.
        val friendIds = evaluatedActive.mapNotNull { it.otherMemberId(uid) }.distinct()
        observeFriendStreaks(friendIds)
    }

    /** Observes each connected friend's individual streak from their user document. */
    private fun observeFriendStreaks(friendIds: List<String>) {
        if (friendIds.isEmpty()) {
            clearFriendListeners()
            friendStreaks = emptyList()
            return
        }

        val expectedIds = friendIds.toSet()
        friendListeners
            .filterKeys { it !in expectedIds }
            .toList()
            .forEach { (friendId, listener) ->
                listener.remove()
                friendListeners.remove(friendId)
                friendStreaksById.remove(friendId)
            }

        friendIds.forEach { friendId ->
            if (friendId in friendListeners) return@forEach

            friendListeners[friendId] = userRepository.observeUser(
                uid = friendId,
                onChange = { friend ->
                    if (friend == null) {
                        friendStreaksById.remove(friendId)
                    } else {
                        friendStreaksById[friendId] = FriendStreak(
                            uid = friend.uid,
                            displayName = friend.displayName.ifBlank { friend.email.substringBefore("@") },
                            currentStreak = friend.progress.currentStreak,
                            lastQuestCompletionDate = friend.progress.lastQuestCompletionDate
                        )
                    }
                    publishFriendStreaks()
                },
                onError = {
                    loadError = "We couldn't keep a friend's streak in sync. Try refreshing."
                }
            )
        }
    }

    private fun publishFriendStreaks() {
        friendStreaks = friendStreaksById.values.sortedByDescending { it.currentStreak }
    }

    private fun clearFriendListeners() {
        friendListeners.values.forEach { it.remove() }
        friendListeners.clear()
        friendStreaksById.clear()
    }

    private fun persist(streak: SharedStreak) {
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) { sharedStreakRepository.updateStreak(streak) }
            }.onFailure { Log.w(TAG, "persist: failed to save shared streak ${streak.id}", it) }
        }
    }

    private fun ensureCurrentUserProfile() {
        val firebaseUser = auth.currentUser ?: return
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    val existing = userRepository.getUser(firebaseUser.uid)
                    val displayName = existing?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: firebaseUser.displayName.orEmpty()
                    val email = existing?.email
                        ?.takeIf { it.isNotBlank() }
                        ?: firebaseUser.email.orEmpty()

                    userRepository.saveUser(
                        (existing ?: User(
                            uid = firebaseUser.uid,
                            displayName = displayName,
                            email = email
                        )).copy(
                            displayName = displayName,
                            email = email
                        )
                    )
                }
            }.onFailure { Log.w(TAG, "ensureCurrentUserProfile: profile sync failed", it) }
        }
    }

    override fun onCleared() {
        streakListener?.remove()
        streakListener = null
        clearFriendListeners()
        super.onCleared()
    }

    private companion object {
        const val FIRESTORE_TIMEOUT_MS = 8_000L
        const val TAG = "SharedStreakVM"
    }
}
