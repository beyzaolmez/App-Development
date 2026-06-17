package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.model.Friendship
import com.nhlstenden.momentum.data.model.FriendshipStatus
import com.nhlstenden.momentum.data.model.FriendUser
import com.nhlstenden.momentum.data.repository.FirestoreFriendRepository
import com.nhlstenden.momentum.data.repository.FriendRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.util.toFriendlyFriendMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FriendViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val friendRepository: FriendRepository = FirestoreFriendRepository()
) : ViewModel() {

    /** Current search query input. */
    var searchQuery by mutableStateOf("")
        private set

    /** Search results (users that can be added as friends). */
    var searchResults by mutableStateOf<List<FriendUser>>(emptyList())
        private set

    /** Whether a search is in progress. */
    var isSearching by mutableStateOf(false)
        private set

    /** All friendships for the current user. */
    var friendships by mutableStateOf<List<Friendship>>(emptyList())
        private set

    /** Friendships I sent that are pending. */
    val outgoingRequests: List<Friendship>
        get() = friendships.filter {
            it.requesterUid == currentUid && it.status == FriendshipStatus.Pending
        }

    /** Friendships I received that are pending. */
    val incomingRequests: List<Friendship>
        get() = friendships.filter {
            it.recipientUid == currentUid && it.status == FriendshipStatus.Pending
        }

    /** Accepted friendships (actual friends). */
    val acceptedFriends: List<Friendship>
        get() = friendships.filter { it.status == FriendshipStatus.Accepted }

    /** Whether data is loading. */
    var isLoading by mutableStateOf(false)
        private set

    /** Error message to display. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Success message for actions. */
    var successMessage by mutableStateOf<String?>(null)
        private set

    private val currentUid: String? get() = auth.currentUser?.uid

    init {
        observeFriendships()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    /** Search for users to add as friends. */
    fun searchUsers() {
        val uid = currentUid ?: run {
            errorMessage = "Sign in to search for friends."
            return
        }
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            searchResults = emptyList()
            return
        }

        viewModelScope.launch {
            isSearching = true
            errorMessage = null

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.searchUsers(query, uid)
                }
            }.onSuccess { results ->
                searchResults = results
                isSearching = false
            }.onFailure { error ->
                errorMessage = "Search failed. Please try again."
                isSearching = false
            }
        }
    }

    /** Send a friend request to a user. */
    fun sendFriendRequest(recipient: FriendUser) {
        val requesterUid = currentUid ?: run {
            errorMessage = "Sign in to add friends."
            return
        }

        viewModelScope.launch {
            errorMessage = null
            successMessage = null

            // Get requester's name
            val requester = runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) { userRepository.getUser(requesterUid) }
            }.getOrNull()

            val requesterName = requester?.displayName?.ifBlank { requester.email.substringBefore("@") }
                ?: auth.currentUser?.displayName
                ?: "A user"

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.sendFriendRequest(
                        requesterUid = requesterUid,
                        requesterName = requesterName,
                        recipientUid = recipient.uid,
                        recipientName = recipient.displayName.ifBlank { recipient.email.substringBefore("@") }
                    )
                }
            }.onSuccess {
                successMessage = "Friend request sent to ${recipient.displayName.ifBlank { recipient.email }}!"
                // Remove from search results since request sent
                searchResults = searchResults.filter { it.uid != recipient.uid }
            }.onFailure { error ->
                errorMessage = "Couldn't send friend request. Please try again."
            }
        }
    }

    /** Accept an incoming friend request. */
    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            errorMessage = null
            successMessage = null

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.acceptFriendRequest(friendshipId)
                }
            }.onSuccess {
                val friend = incomingRequests.find { it.id == friendshipId }
                successMessage = "You and ${friend?.requesterName ?: "friend"} are now connected!"
            }.onFailure { error ->
                errorMessage = "Couldn't accept request. Please try again."
            }
        }
    }

    /** Decline an incoming friend request. */
    fun declineRequest(friendshipId: String) {
        viewModelScope.launch {
            errorMessage = null

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.declineFriendRequest(friendshipId)
                }
            }.onFailure { error ->
                errorMessage = "Couldn't decline request. Please try again."
            }
        }
    }

    /** Cancel an outgoing friend request. */
    fun cancelRequest(friendshipId: String) {
        viewModelScope.launch {
            errorMessage = null

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.removeFriend(friendshipId)
                }
            }.onFailure { error ->
                errorMessage = "Couldn't cancel request. Please try again."
            }
        }
    }

    /** Remove an existing friendship. */
    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            errorMessage = null
            successMessage = null

            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    friendRepository.removeFriend(friendshipId)
                }
            }.onSuccess {
                successMessage = "Friend removed."
            }.onFailure { error ->
                errorMessage = "Couldn't remove friend. Please try again."
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun clearSuccess() {
        successMessage = null
    }

    private fun observeFriendships() {
        val uid = currentUid ?: return

        friendRepository.observeFriendships(
            uid = uid,
            onChange = { updatedFriendships ->
                friendships = updatedFriendships
            },
            onError = { error ->
                errorMessage = "Couldn't load friends. Pull to refresh."
            }
        )
    }

    private companion object {
        const val FIRESTORE_TIMEOUT_MS = 8_000L
    }
}
