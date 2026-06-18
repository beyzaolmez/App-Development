package com.nhlstenden.momentum.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhlstenden.momentum.data.model.FriendStreak
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.navigation.FriendsTabs
import com.nhlstenden.momentum.ui.components.ChipVariant
import com.nhlstenden.momentum.ui.components.MomentumCard
import com.nhlstenden.momentum.ui.components.MomentumChip
import com.nhlstenden.momentum.ui.components.MomentumInlineError
import com.nhlstenden.momentum.ui.components.MomentumPrimaryButton
import com.nhlstenden.momentum.ui.components.MomentumSecondaryButton
import com.nhlstenden.momentum.ui.components.MomentumStatusCard
import com.nhlstenden.momentum.ui.components.MomentumTextField
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import com.nhlstenden.momentum.viewmodel.SharedStreakViewModel

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onOpenQuestBoard: () -> Unit = {},
    sharedStreakViewModel: SharedStreakViewModel = viewModel()
) {
    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    // Track selected tab (0 = Connections, 1 = Streaks, 2 = Find)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Invite state for Find Friends tab (using direct invite like old working tab)
    var inviteEmail by remember { mutableStateOf("") }

    FriendsScreenWithTabs(
        currentUid = currentUid,
        isSignedIn = sharedStreakViewModel.isSignedIn,
        isLoading = sharedStreakViewModel.isLoading,
        loadError = sharedStreakViewModel.loadError,
        activeStreaks = sharedStreakViewModel.activeStreaks,
        friendStreaks = sharedStreakViewModel.friendStreaks,
        incomingInvitations = sharedStreakViewModel.incomingInvitations,
        outgoingInvitations = sharedStreakViewModel.outgoingInvitations,
        declinedInvitations = sharedStreakViewModel.declinedInvitations,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onBack = onBack,
        // Streak actions
        onRefresh = sharedStreakViewModel::refresh,
        onAccept = sharedStreakViewModel::accept,
        onDecline = sharedStreakViewModel::decline,
        // Find Friends tab (direct invite like old working tab)
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
            inviteEmail = ""
        },
        onOpenQuestBoard = onOpenQuestBoard,
        onClearInviteFeedback = sharedStreakViewModel::clearInviteFeedback
    )
}

@Composable
private fun FriendsScreenWithTabs(
    currentUid: String?,
    isSignedIn: Boolean,
    isLoading: Boolean,
    loadError: String?,
    activeStreaks: List<SharedStreak>,
    friendStreaks: List<FriendStreak>,
    incomingInvitations: List<SharedStreak>,
    outgoingInvitations: List<SharedStreak>,
    declinedInvitations: List<SharedStreak>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    // Find Friends tab (uses direct invite like old working tab)
    inviteEmail: String,
    onInviteEmailChange: (String) -> Unit,
    inviteInProgress: Boolean,
    inviteError: String?,
    inviteSuccess: String?,
    onSendInvite: () -> Unit,
    onOpenQuestBoard: () -> Unit,
    onClearInviteFeedback: () -> Unit
) {
    Scaffold(
        topBar = {
            // Header with back button and title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "Friends",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        bottomBar = {
            // Tab navigation
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                FriendsTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { 
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            when (selectedTab) {
                0 -> ConnectionsTab(
                    currentUid = currentUid,
                    isSignedIn = isSignedIn,
                    activeStreaks = activeStreaks,
                    friendStreaks = friendStreaks,
                    onOpenQuestBoard = onOpenQuestBoard
                )
                1 -> StreaksTab(
                    currentUid = currentUid,
                    isSignedIn = isSignedIn,
                    isLoading = isLoading,
                    loadError = loadError,
                    activeStreaks = activeStreaks,
                    incomingInvitations = incomingInvitations,
                    outgoingInvitations = outgoingInvitations,
                    declinedInvitations = declinedInvitations,
                    onGoToFindFriends = { onTabSelected(2) },
                    onRefresh = onRefresh,
                    onAccept = onAccept,
                    onDecline = onDecline
                )
                2 -> FindFriendsTab(
                    isSignedIn = isSignedIn,
                    inviteEmail = inviteEmail,
                    onInviteEmailChange = onInviteEmailChange,
                    inviteInProgress = inviteInProgress,
                    inviteError = inviteError,
                    inviteSuccess = inviteSuccess,
                    onSendInvite = onSendInvite,
                    onClearInviteFeedback = onClearInviteFeedback
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConnectionsTab(
    currentUid: String?,
    isSignedIn: Boolean,
    activeStreaks: List<SharedStreak>,
    friendStreaks: List<FriendStreak>,
    onOpenQuestBoard: () -> Unit
) {
    if (!isSignedIn) {
        MomentumStatusCard(
            title = "Sign in required",
            message = "Sign in to see your friends and their progress.",
            variant = ChipVariant.Neutral
        )
        return
    }
    
    val allFriends = activeStreaks.map { streak ->
        // otherMemberName takes the CURRENT user's uid and returns the other member's name.
        val otherName = currentUid?.let { streak.otherMemberName(it) } ?: "Friend"
        otherName to streak.currentStreak
    }.distinct()
    
    if (allFriends.isEmpty()) {
        MomentumStatusCard(
            title = "No friends yet",
            message = "Go to 'Find' tab to search for friends and start a shared streak.",
            variant = ChipVariant.Neutral
        )
    } else {
        Text(
            "Your friends (${allFriends.size})",
            style = MaterialTheme.typography.titleLarge
        )
        
        allFriends.forEach { (name, streakCount) ->
            FriendConnectionRow(
                name = name,
                sharedStreakCount = streakCount
            )
        }
    }

    MomentumSecondaryButton(
        text = "See friend-liked quests",
        onClick = onOpenQuestBoard,
        modifier = Modifier.fillMaxWidth()
    )
    
    // Personal streaks of friends
    if (friendStreaks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "How they're doing",
            style = MaterialTheme.typography.titleLarge
        )
        friendStreaks.forEach { friend ->
            FriendPersonalStreakRow(friend)
        }
    }
}

@Composable
private fun StreaksTab(
    currentUid: String?,
    isSignedIn: Boolean,
    isLoading: Boolean,
    loadError: String?,
    activeStreaks: List<SharedStreak>,
    incomingInvitations: List<SharedStreak>,
    outgoingInvitations: List<SharedStreak>,
    declinedInvitations: List<SharedStreak>,
    onGoToFindFriends: () -> Unit,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    if (!isSignedIn) {
        MomentumStatusCard(
            title = "Sign in required",
            message = "Sign in to start and manage shared streaks with friends.",
            variant = ChipVariant.Neutral
        )
        return
    }
    
    // Header with refresh and invite button
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MomentumSecondaryButton(
            text = if (isLoading) "..." else "Refresh",
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
            enabled = !isLoading
        )
        MomentumPrimaryButton(
            text = "+ Invite friend",
            onClick = onGoToFindFriends,
            modifier = Modifier.weight(1f)
        )
    }
    
    if (loadError != null) {
        MomentumInlineError(loadError)
    }
    
    // Pending invitations
    if (incomingInvitations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Invitations to accept (${incomingInvitations.size})",
            style = MaterialTheme.typography.titleLarge
        )
        incomingInvitations.forEach { streak ->
            IncomingInvitationRow(
                name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend",
                onAccept = { onAccept(streak.id) },
                onDecline = { onDecline(streak.id) }
            )
        }
    }
    
    // Active streaks
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        if (activeStreaks.isEmpty()) "No active streaks" 
        else "Active streaks (${activeStreaks.size})",
        style = MaterialTheme.typography.titleLarge
    )
    
    if (activeStreaks.isEmpty() && !isLoading) {
        MomentumStatusCard(
            title = "No streaks yet",
            message = "Go to the Find tab to search for friends and start your first shared streak.",
            variant = ChipVariant.Neutral
        )
        MomentumSecondaryButton(
            text = "Find friends",
            onClick = onGoToFindFriends,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    activeStreaks.forEach { streak ->
        SharedStreakCard(
            name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend",
            streak = streak,
            currentUid = currentUid
        )
    }
    
    // Outgoing invitations
    if (outgoingInvitations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Waiting for response (${outgoingInvitations.size})",
            style = MaterialTheme.typography.titleLarge
        )
        outgoingInvitations.forEach { streak ->
            OutgoingInvitationRow(
                name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend"
            )
        }
    }
    
    // Declined invitations
    if (declinedInvitations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Declined (${declinedInvitations.size})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        declinedInvitations.forEach { streak ->
            DeclinedInvitationRow(
                name = currentUid?.let { streak.otherMemberName(it) } ?: "Friend"
            )
        }
    }
}

@Composable
private fun FindFriendsTab(
    isSignedIn: Boolean,
    inviteEmail: String,
    onInviteEmailChange: (String) -> Unit,
    inviteInProgress: Boolean,
    inviteError: String?,
    inviteSuccess: String?,
    onSendInvite: () -> Unit,
    onClearInviteFeedback: () -> Unit
) {
    if (!isSignedIn) {
        MomentumStatusCard(
            title = "Sign in required",
            message = "Sign in to find friends and start shared streaks.",
            variant = ChipVariant.Neutral
        )
        return
    }
    
    Text(
        "Find friends",
        style = MaterialTheme.typography.titleLarge
    )
    Text(
        "Enter your friend's email to invite them to a shared streak.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    MomentumTextField(
        value = inviteEmail,
        onValueChange = {
            onInviteEmailChange(it)
            onClearInviteFeedback()
        },
        placeholder = "friend@example.com",
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
}

@Composable
private fun FriendConnectionRow(
    name: String,
    sharedStreakCount: Int
) {
    MomentumCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sharedStreakCount > 0) "${sharedStreakCount} day streak together · Shared"
                    else "No active streak yet · Shared",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sharedStreakCount > 0) {
                MomentumChip(
                    "${sharedStreakCount}d",
                    variant = ChipVariant.Skills
                )
            }
        }
    }
}

@Composable
private fun FriendPersonalStreakRow(friend: FriendStreak) {
    MomentumCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(friend.displayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        friend.currentStreak <= 0 -> "No personal streak yet · Personal"
                        friend.currentStreak == 1 -> "On a 1 day personal streak · Personal"
                        else -> "On a ${friend.currentStreak} day personal streak · Personal"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (friend.currentStreak > 0) {
                MomentumChip(
                    "${friend.currentStreak}d",
                    variant = ChipVariant.Category
                )
            }
        }
    }
}

@Composable
private fun IncomingInvitationRow(
    name: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    MomentumCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Invited you to a shared streak",
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
private fun OutgoingInvitationRow(name: String) {
    MomentumCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Waiting for them to accept",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MomentumChip("Pending", variant = ChipVariant.Neutral)
        }
    }
}

@Composable
private fun DeclinedInvitationRow(name: String) {
    MomentumCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
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

@Composable
private fun SharedStreakCard(
    name: String,
    streak: SharedStreak,
    currentUid: String?
) {
    val today = SharedStreakLogic.today()
    val otherUid = currentUid?.let { streak.otherMemberId(it) }
    val youDoneToday = currentUid?.let { streak.lastCompletionDates[it] == today } == true
    val friendDoneToday = otherUid?.let { streak.lastCompletionDates[it] == today } == true
    
    val bothDoneToday = youDoneToday && friendDoneToday
    val isStreakAtRisk = !bothDoneToday && streak.currentStreak > 0

    MomentumCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("With $name", style = MaterialTheme.typography.titleLarge)
                if (streak.currentStreak > 0) {
                    MomentumChip(
                        "${streak.currentStreak}d",
                        variant = if (isStreakAtRisk) ChipVariant.Neutral else ChipVariant.Skills
                    )
                } else {
                    MomentumChip("New", variant = ChipVariant.Reward)
                }
            }
            
            // Status row with clearer messaging
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MomentumChip(
                    if (youDoneToday) "✓ You" else "○ You",
                    variant = if (youDoneToday) ChipVariant.Skills else ChipVariant.Neutral
                )
                MomentumChip(
                    if (friendDoneToday) "✓ Them" else "○ Them",
                    variant = if (friendDoneToday) ChipVariant.Skills else ChipVariant.Neutral
                )
            }
            
            // Description text
            Text(
                when {
                    streak.currentStreak == 0 ->
                        "Complete a quest each to start your streak."
                    bothDoneToday ->
                        "Both done today — streak secured!"
                    isStreakAtRisk ->
                        "Complete a quest today to keep the streak alive."
                    else ->
                        "Waiting for both to complete today's quest."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isStreakAtRisk -> MaterialTheme.colorScheme.error
                    bothDoneToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, widthDp = 360, heightDp = 720)
@Composable
private fun FriendsPreview() {
    MomentumTheme {
        FriendsScreenWithTabs(
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
                    lastCompletionDates = mapOf("me" to SharedStreakLogic.today()),
                    lastIncrementDate = SharedStreakLogic.today()
                ),
                SharedStreak(
                    id = "3",
                    memberIds = listOf("me", "alex"),
                    memberNames = mapOf("me" to "You", "alex" to "Alex"),
                    status = SharedStreakStatus.Active,
                    currentStreak = 0,
                    lastCompletionDates = emptyMap(),
                    lastIncrementDate = null
                )
            ),
            friendStreaks = listOf(
                FriendStreak(uid = "noor", displayName = "Noor", currentStreak = 5),
                FriendStreak(uid = "alex", displayName = "Alex", currentStreak = 2)
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
            outgoingInvitations = listOf(
                SharedStreak(
                    id = "4",
                    memberIds = listOf("me", "taylor"),
                    memberNames = mapOf("me" to "You", "taylor" to "Taylor"),
                    status = SharedStreakStatus.Pending,
                    invitedByUid = "me"
                )
            ),
            declinedInvitations = emptyList(),
            selectedTab = 1, // Preview Streaks tab
            onTabSelected = {},
            onBack = {},
            onRefresh = {},
            onAccept = {},
            onDecline = {},
            // Find Friends
            inviteEmail = "",
            onInviteEmailChange = {},
            inviteInProgress = false,
            inviteError = null,
            inviteSuccess = null,
            onSendInvite = {},
            onOpenQuestBoard = {},
            onClearInviteFeedback = {}
        )
    }
}
