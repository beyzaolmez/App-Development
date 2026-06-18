package com.nhlstenden.momentum.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedStreakLogicTest {

    private val activeStreak = SharedStreak(
        id = "streak-1",
        memberIds = listOf("alice", "bob"),
        memberNames = mapOf("alice" to "Alice", "bob" to "Bob"),
        status = SharedStreakStatus.Active,
        invitedByUid = "alice"
    )

    @Test
    fun recordCompletionStoresFirstMemberWithoutIncrementing() {
        val updated = SharedStreakLogic.recordCompletion(activeStreak, "alice", "2026-06-17")

        assertEquals("2026-06-17", updated.lastCompletionDates["alice"])
        assertEquals(0, updated.currentStreak)
        assertNull(updated.lastIncrementDate)
    }

    @Test
    fun recordCompletionIncrementsOnceWhenBothMembersCompleteSameDay() {
        val first = SharedStreakLogic.recordCompletion(activeStreak, "alice", "2026-06-17")
        val second = SharedStreakLogic.recordCompletion(first, "bob", "2026-06-17")
        val duplicate = SharedStreakLogic.recordCompletion(second, "alice", "2026-06-17")

        assertEquals(1, second.currentStreak)
        assertEquals("2026-06-17", second.lastIncrementDate)
        assertEquals(second, duplicate)
    }

    @Test
    fun recordCompletionContinuesRunFromPreviousDay() {
        val existing = activeStreak.copy(
            currentStreak = 4,
            lastIncrementDate = "2026-06-16",
            lastCompletionDates = mapOf("alice" to "2026-06-16", "bob" to "2026-06-16")
        )

        val first = SharedStreakLogic.recordCompletion(existing, "alice", "2026-06-17")
        val second = SharedStreakLogic.recordCompletion(first, "bob", "2026-06-17")

        assertEquals(5, second.currentStreak)
        assertEquals("2026-06-17", second.lastIncrementDate)
    }

    @Test
    fun evaluateForTodayBreaksStreakAfterMissedFullDay() {
        val existing = activeStreak.copy(
            currentStreak = 4,
            lastIncrementDate = "2026-06-15"
        )

        val evaluated = SharedStreakLogic.evaluateForToday(existing, "2026-06-17")

        assertEquals(0, evaluated.currentStreak)
        assertEquals("2026-06-15", evaluated.lastIncrementDate)
    }

    @Test
    fun pendingStreaksDoNotChange() {
        val pending = activeStreak.copy(status = SharedStreakStatus.Pending)

        val updated = SharedStreakLogic.recordCompletion(pending, "alice", "2026-06-17")
        val evaluated = SharedStreakLogic.evaluateForToday(pending, "2026-06-17")
        val liked = SharedStreakLogic.recordLike(pending, "alice", "quest-1")

        assertEquals(pending, updated)
        assertEquals(pending, evaluated)
        assertEquals(pending, liked)
    }

    @Test
    fun recordCompletionCanCountExistingSameDayCompletionsOnActivation() {
        val activated = activeStreak.copy(
            lastCompletionDates = mapOf("alice" to "2026-06-17", "bob" to "2026-06-17")
        )

        val updated = SharedStreakLogic.recordCompletion(activated, "alice", "2026-06-17")

        assertEquals(1, updated.currentStreak)
        assertEquals("2026-06-17", updated.lastIncrementDate)
    }

    @Test
    fun recordLikeStoresUniqueQuestIdsForActiveMembers() {
        val first = SharedStreakLogic.recordLike(activeStreak, "alice", "quest-1")
        val duplicate = SharedStreakLogic.recordLike(first, "alice", "quest-1")
        val second = SharedStreakLogic.recordLike(duplicate, "bob", "quest-2")

        assertEquals(listOf("quest-1"), second.likedQuestIdsFor("alice"))
        assertEquals(listOf("quest-2"), second.likedQuestIdsFor("bob"))
    }
}
