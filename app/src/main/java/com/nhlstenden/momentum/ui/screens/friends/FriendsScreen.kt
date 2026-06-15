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
import com.nhlstenden.momentum.viewmodel.SharedStreakViewModel

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    viewModel: SharedStreakViewModel = viewModel()
) {
    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var inviteEmail by remember { mutableStateOf("") }

    FriendsContent(
        currentUid = currentUid,
        isSignedIn = viewModel.isSignedIn,
        isLoading = viewModel.isLoading,
        loadError = viewModel.loadError,
        activeStreaks = viewModel.activeStreaks,
        incomingInvitations = viewModel.incomingInvitations,
        outgoingInvitations = viewModel.outgoingInvitations,
        inviteEmail = inviteEmail,
        onInviteEmailChange = {
            inviteEmail = it
            viewModel.clearInviteFeedback()
        },
        inviteInProgress = viewModel.inviteInProgress,
        inviteError = viewModel.inviteError,
        inviteSuccess = viewModel.inviteSuccess,
        onSendInvite = {
            viewModel.invite(inviteEmail)
        },
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onBack = onBack
    )
}

@Composable
private fun FriendsContent(
    currentUid: String?,
    isSignedIn: Boolean,
    isLoading: Boolean,
    loadError: String?,
    activeStreaks: List<SharedStreak>,
    incomingInvitations: List<SharedStreak>,
    outgoingInvitations: List<SharedStreak>,
    inviteEmail: String,
    onInviteEmailChange: (String) -> Unit,
    inviteInProgress: Boolean,
    inviteError: String?,
    inviteSuccess: String?,
    onSendInvite: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onBack: () -> Unit
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
            inviteEmail = "",
            onInviteEmailChange = {},
            inviteInProgress = false,
            inviteError = null,
            inviteSuccess = null,
            onSendInvite = {},
            onAccept = {},
            onDecline = {},
            onBack = {}
        )
    }
}