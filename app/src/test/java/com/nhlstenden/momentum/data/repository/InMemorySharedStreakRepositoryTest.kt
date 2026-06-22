package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySharedStreakRepositoryTest {

    @Test
    fun cancelInvitationRemovesPendingInvitation() = runBlocking {
        val repository = InMemorySharedStreakRepository()
        val streakId = repository.createInvitation(
            inviterUid = "alice",
            inviterName = "Alice",
            inviteeUid = "bob",
            inviteeName = "Bob"
        )

        repository.cancelInvitation(streakId)

        assertTrue(repository.getStreaksForUser("alice").isEmpty())
        assertTrue(repository.getStreaksForUser("bob").isEmpty())
    }

    @Test
    fun cancelInvitationDoesNotRemoveActiveStreak() = runBlocking {
        val repository = InMemorySharedStreakRepository()
        val activeStreak = SharedStreak(
            id = "active-1",
            memberIds = listOf("alice", "bob"),
            memberNames = mapOf("alice" to "Alice", "bob" to "Bob"),
            status = SharedStreakStatus.Active,
            invitedByUid = "alice"
        )

        repository.updateStreak(activeStreak)
        repository.cancelInvitation(activeStreak.id)

        assertEquals(listOf(activeStreak), repository.getStreaksForUser("alice"))
    }
}
