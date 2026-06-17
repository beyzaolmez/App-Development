package com.nhlstenden.momentum.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhlstenden.momentum.data.model.Friendship
import com.nhlstenden.momentum.data.model.FriendshipStatus
import com.nhlstenden.momentum.data.model.FriendStreak
import com.nhlstenden.momentum.data.model.FriendUser
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumInlineError
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumQuietButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.components.MomentumStatusCard
import com.nhlstenden.momentum.ui.components.MomentumTextField
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.viewmodel.FriendViewModel
import com.nhlstenden.momentum.viewmodel.SharedStreakViewModel

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    sharedStreakViewModel: SharedStreakViewModel = viewModel(),
    friendViewModel: FriendViewModel = viewModel()
) {
    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var inviteEmail by remember { mutableStateOf("") }

    FriendsContent(
        currentUid = currentUid,
        isSignedIn = sharedStreakViewModel.isSignedIn,
        isLoading = sharedStreakViewModel.isLoading,
        loadError = sharedStreakViewModel.loadError,
        activeStreaks = sharedStreakViewModel.activeStreaks,
        friendStreaks = sharedStreakViewModel.friendStreaks,
        incomingInvitations = sharedStreakViewModel.incomingInvitations,
        outgoingInvitations = sharedStreakViewModel.outgoingInvitations,
        declinedInvitations = sharedStreakViewModel.declinedInvitations,
        inviteEmail = inviteEmail,
        onInviteEmailChange = {
            inviteEmail = it
            sharedStreakViewModel.clearInviteFeedback()
        },
        inviteInProgress = sharedStreakViewModel.inviteInProgress,
        inviteError = sharedStreakViewModel.inviteError,
        inviteSuccess = sharedStreakViewModel.inviteSuccess,
        onSendInvite = {
            sharedStreakViewModel.invite(inviteEmail)
        },
        onRefresh = sharedStreakViewModel::refresh,
        onAccept = sharedStreakViewModel::accept,
        onDecline = sharedStreakViewModel::decline,
        onBack = onBack,
        // Friend system
        friendSearchQuery = friendViewModel.searchQuery,
        onSearchQueryChange = friendViewModel::updateSearchQuery,
        onSearchUsers = friendViewModel::searchUsers,
        searchResults = friendViewModel.searchResults,
        isSearching = friendViewModel.isSearching,
        onSendFriendRequest = friendViewModel::sendFriendRequest,
        incomingFriendRequests = friendViewModel.incomingRequests,
        outgoingFriendRequests = friendViewModel.outgoingRequests,
        acceptedFriends = friendViewModel.acceptedFriends,
        onAcceptFriendRequest = friendViewModel::acceptRequest,
        onDeclineFriendRequest = friendViewModel::declineRequest,
        onCancelFriendRequest = friendViewModel::cancelRequest,
        onRemoveFriend = friendViewModel::removeFriend,
        friendError = friendViewModel.errorMessage,
        friendSuccess = friendViewModel.successMessage,
        onClearFriendError = friendViewModel::clearError,
        onClearFriendSuccess = friendViewModel::clearSuccess
    )
}

@Composable
private fun FriendsContent(
    currentUid: String?,
    isSignedIn: Boolean,
    isLoading: Boolean,
    loadError: String?,
    activeStreaks: List<SharedStreak>,
    friendStreaks: List<FriendStreak>,
    incomingInvitations: List<SharedStreak>,
    outgoingInvitations: List<SharedStreak>,
    declinedInvitations: List<SharedStreak>,
    inviteEmail: String,
    onInviteEmailChange: (String) -> Unit,
    inviteInProgress: Boolean,
    inviteError: String?,
    inviteSuccess: String?,
    onSendInvite: () -> Unit,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onBack: () -> Unit,
    // Friend system parameters
    friendSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchUsers: () -> Unit,
    searchResults: List<FriendUser>,
    isSearching: Boolean,
    onSendFriendRequest: (FriendUser) -> Unit,
    incomingFriendRequests: List<Friendship>,
    outgoingFriendRequests: List<Friendship>,
    acceptedFriends: List<Friendship>,
    onAcceptFriendRequest: (String) -> Unit,
    onDeclineFriendRequest: (String) -> Unit,
    onCancelFriendRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    friendError: String?,
    friendSuccess: String?,
    onClearFriendError: () -> Unit,
    onClearFriendSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Friends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Shared streaks", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Team up with a friend. Your shared streak grows on every day you both complete a quest — and resets if a day is missed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isSignedIn) {
            MomentumStatusCard(
                title = "Sign in required",
                message = "Shared streaks connect two accounts, so you'll need to sign in to invite a friend.",
                variant = ChipVariant.Neutral
            )
            MomentumQuietButton("Back to profile", onBack, Modifier.fillMaxWidth())
            return@Column
        }

        if (loadError != null) {
            MomentumInlineError(loadError)
        }

        // ============================================
        // FRIEND SYSTEM SECTION
        // ============================================

        Text("Find friends", style = MaterialTheme.typography.titleLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MomentumTextField(
                value = friendSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search by name or email",
                modifier = Modifier.weight(1f)
            )
            MomentumSecondaryButton(
                text = if (isSearching) "..." else "Search",
                onClick = onSearchUsers,
                enabled = !isSearching && friendSearchQuery.isNotBlank()
            )
        }

        // Search results
        if (searchResults.isNotEmpty()) {
            Text("Search results", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            searchResults.forEach { user ->
                SearchResultRow(
                    user = user,
                    onAddFriend = { onSendFriendRequest(user) }
                )
            }
        }

        if (friendSuccess != null) {
            Text(
                friendSuccess,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (friendError != null) {
            MomentumInlineError(friendError)
        }

        // ---- Incoming friend requests ----
        if (incomingFriendRequests.isNotEmpty()) {
            Text("Friend requests", style = MaterialTheme.typography.titleLarge)
            incomingFriendRequests.forEach { request ->
                FriendRequestRow(
                    name = request.requesterName,
                    onAccept = { onAcceptFriendRequest(request.id) },
                    onDecline = { onDeclineFriendRequest(request.id) }
                )
            }
        }

        // ---- Friends list ----
        Text("My friends (${acceptedFriends.size})", style = MaterialTheme.typography.titleLarge)
        if (acceptedFriends.isEmpty() && outgoingFriendRequests.isEmpty()) {
            MomentumStatusCard(
                title = "No friends yet",
                message = "Search for users above to add friends and connect socially.",
                variant = ChipVariant.Neutral
            )
        }
        acceptedFriends.forEach { friendship ->
            val friendName = if (friendship.requesterUid == currentUid) friendship.recipientName else friendship.requesterName
            FriendListRow(
                name = friendName,
                onRemove = { onRemoveFriend(friendship.id) }
            )
        }

        // ---- Outgoing friend requests ----
        outgoingFriendRequests.forEach { request ->
            MomentumCard {
                Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                request.recipientName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Friend request sent · waiting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MomentumChip("Pending", variant = ChipVariant.Neutral)
                    }
                    MomentumQuietButton(
                        "Cancel request",
                        { onCancelFriendRequest(request.id) },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ============================================
        // SHARED STREAKS SECTION
        // ============================================

        MomentumSecondaryButton(
            text = if (isLoading) "Refreshing..." else "Refresh streaks",
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // ---- Incoming invitations ----
        if (incomingInvitations.isNotEmpty()) {
            Text("Invitations", style = MaterialTheme.typography.titleLarge)
            incomingInvitations.forEach { streak ->
                InvitationRow(
                    name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend",
                    onAccept = { onAccept(streak.id) },
                    onDecline = { onDecline(streak.id) }
                )
            }
        }

        // ---- Active shared streaks ----
        Text("Your shared streaks", style = MaterialTheme.typography.titleLarge)
        if (activeStreaks.isEmpty() && !isLoading) {
            MomentumStatusCard(
                title = "No shared streaks yet",
                message = "Invite a friend below to start your first shared streak.",
                variant = ChipVariant.Neutral
            )
        }
        activeStreaks.forEach { streak ->
            SharedStreakRow(
                name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend",
                streak = streak,
                bothDoneToday = streak.bothCompletedOn(SharedStreakLogic.today())
            )
        }

        // ---- Friends' own streaks ----
        // Shows each connected friend's personal quest streak so the user can see
        // how their friends are doing and stay motivated.
        if (friendStreaks.isNotEmpty()) {
            Text("How your friends are doing", style = MaterialTheme.typography.titleLarge)
            friendStreaks.forEach { friend ->
                FriendStreakRow(friend)
            }
        }

        // ---- Outgoing (waiting) invitations ----
        outgoingInvitations.forEach { streak ->
            MomentumCard {
                Row(
                    modifier = it,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            currentUid?.let { uid -> streak.otherMemberName(uid) } ?: "Friend",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Invitation sent · waiting to accept",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MomentumChip("Pending", variant = ChipVariant.Neutral)
                }
            }
        }

        // ---- Declined invitations ----
        declinedInvitations.forEach { streak ->
            MomentumCard {
                Row(
                    modifier = it,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            currentUid?.let { uid -> streak.otherMemberName(uid) } ?: "Friend",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Invitation declined",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    MomentumChip("Declined", variant = ChipVariant.Neutral)
                }
            }
        }

        // ---- Invite a friend ----
        Text("Invite a friend", style = MaterialTheme.typography.titleLarge)
        MomentumTextField(
            value = inviteEmail,
            onValueChange = onInviteEmailChange,
            placeholder = "Friend's email address",
            keyboardType = KeyboardType.Email,
            errorText = inviteError
        )
        if (inviteSuccess != null) {
            Text(
                inviteSuccess,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        MomentumPrimaryButton(
            text = if (inviteInProgress) "Sending…" else "Send invitation",
            onClick = onSendInvite,
            modifier = Modifier.fillMaxWidth(),
            enabled = inviteEmail.isNotBlank() && !inviteInProgress
        )
        MomentumQuietButton("Back to profile", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun InvitationRow(
    name: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "wants to maintain a shared streak with you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumPrimaryButton("Accept", onAccept, Modifier.weight(1f))
                MomentumSecondaryButton("Decline", onDecline, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SharedStreakRow(
    name: String,
    streak: SharedStreak,
    bothDoneToday: Boolean
) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shared with $name", style = MaterialTheme.typography.titleLarge)
                if (streak.currentStreak > 0) {
                    MomentumChip(
                        "${streak.currentStreak} ${if (streak.currentStreak == 1) "day" else "days"}",
                        variant = ChipVariant.Skills
                    )
                } else {
                    MomentumChip("new", variant = ChipVariant.Reward)
                }
            }
            Text(
                when {
                    streak.currentStreak == 0 ->
                        "Complete a quest each on the same day to start your streak."
                    bothDoneToday ->
                        "You both completed a quest today — streak is safe. Keep it going!"
                    else ->
                        "Both of you need to complete a quest today to keep the streak alive."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FriendStreakRow(friend: FriendStreak) {
    MomentumCard {
        Row(
            modifier = it,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(friend.displayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        friend.currentStreak <= 0 -> "No streak yet — cheer them on"
                        friend.currentStreak == 1 -> "On a 1 day streak"
                        else -> "On a ${friend.currentStreak} day streak"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (friend.currentStreak > 0) {
                MomentumChip(
                    "${friend.currentStreak} ${if (friend.currentStreak == 1) "day" else "days"}",
                    variant = ChipVariant.Category
                )
            } else {
                MomentumChip("new", variant = ChipVariant.Reward)
            }
        }
    }
}

// ============================================
// FRIEND SYSTEM COMPOSABLES
// ============================================

@Composable
private fun SearchResultRow(
    user: FriendUser,
    onAddFriend: () -> Unit
) {
    MomentumCard {
        Row(
            modifier = it,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MomentumPrimaryButton("Add friend", onAddFriend)
        }
    }
}

@Composable
private fun FriendRequestRow(
    name: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    MomentumCard {
        Column(it, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "wants to be friends with you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumPrimaryButton("Accept", onAccept, Modifier.weight(1f))
                MomentumSecondaryButton("Decline", onDecline, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FriendListRow(
    name: String,
    onRemove: () -> Unit
) {
    MomentumCard {
        Row(
            modifier = it,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
            MomentumQuietButton("Remove", onRemove)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun FriendsPreview() {
    MomentumTheme {
        FriendsContent(
            currentUid = "me",
            isSignedIn = true,
            isLoading = false,
            loadError = null,
            activeStreaks = listOf(
                SharedStreak(
                    id = "1",
                    memberIds = listOf("me", "noor"),
                    memberNames = mapOf("me" to "You", "noor" to "Noor"),
                    status = SharedStreakStatus.Active,
                    currentStreak = 3,
                    lastIncrementDate = SharedStreakLogic.today()
                )
            ),
            friendStreaks = listOf(
                FriendStreak(uid = "noor", displayName = "Noor", currentStreak = 5),
                FriendStreak(uid = "sam", displayName = "Sam", currentStreak = 0)
            ),
            incomingInvitations = listOf(
                SharedStreak(
                    id = "2",
                    memberIds = listOf("sam", "me"),
                    memberNames = mapOf("sam" to "Sam", "me" to "You"),
                    status = SharedStreakStatus.Pending,
                    invitedByUid = "sam"
                )
            ),
            outgoingInvitations = emptyList(),
            declinedInvitations = emptyList(),
            inviteEmail = "",
            onInviteEmailChange = {},
            inviteInProgress = false,
            inviteError = null,
            inviteSuccess = null,
            onSendInvite = {},
            onRefresh = {},
            onAccept = {},
            onDecline = {},
            onBack = {},
            // Friend system preview params
            friendSearchQuery = "",
            onSearchQueryChange = {},
            onSearchUsers = {},
            searchResults = listOf(
                FriendUser(uid = "alex", displayName = "Alex Johnson", email = "alex@example.com")
            ),
            isSearching = false,
            onSendFriendRequest = {},
            incomingFriendRequests = listOf(
                Friendship(
                    id = "fr1",
                    requesterUid = "jordan",
                    requesterName = "Jordan Smith",
                    recipientUid = "me",
                    recipientName = "You",
                    status = FriendshipStatus.Pending
                )
            ),
            outgoingFriendRequests = emptyList(),
            acceptedFriends = listOf(
                Friendship(
                    id = "fr2",
                    requesterUid = "me",
                    requesterName = "You",
                    recipientUid = "taylor",
                    recipientName = "Taylor Brown",
                    status = FriendshipStatus.Accepted,
                    acceptedAt = System.currentTimeMillis()
                )
            ),
            onAcceptFriendRequest = {},
            onDeclineFriendRequest = {},
            onCancelFriendRequest = {},
            onRemoveFriend = {},
            friendError = null,
            friendSuccess = null,
            onClearFriendError = {},
            onClearFriendSuccess = {}
        )
    }
}
