package com.nhlstenden.momentum.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermQuestProgressTest {

    private val longTermQuest = Quest(
        id = "five-study-sessions",
        title = "Complete 5 focused study sessions",
        description = "Build a steady study rhythm.",
        category = QuestCategory.Focus,
        xp = 350,
        difficulty = QuestDifficulty.Hard,
        estimatedMinutes = 25,
        steps = emptyList(),
        goalType = QuestGoalType.LongTerm,
        targetProgress = 5,
        progressUnit = "sessions"
    )

    @Test
    fun updateQuestProgressStartsLongTermQuest() {
        val update = LongTermQuestProgress.updatedQuest(longTermQuest, progressDelta = 1, now = 10L)

        assertEquals(1, update.quest.currentProgress)
        assertEquals(QuestStatus.Active, update.quest.status)
        assertFalse(update.completedNow)
        assertEquals(10L, update.updatedAt)
    }

    @Test
    fun updateQuestProgressCompletesWhenTargetIsReached() {
        val activeQuest = longTermQuest.copy(currentProgress = 4, status = QuestStatus.Active)

        val update = LongTermQuestProgress.updatedQuest(activeQuest, progressDelta = 1, now = 10L)

        assertEquals(5, update.quest.currentProgress)
        assertEquals(QuestStatus.Completed, update.quest.status)
        assertTrue(update.completedNow)
    }

    @Test
    fun updateQuestProgressDoesNotExceedTarget() {
        val activeQuest = longTermQuest.copy(currentProgress = 4, status = QuestStatus.Active)

        val update = LongTermQuestProgress.updatedQuest(activeQuest, progressDelta = 4, now = 10L)

        assertEquals(5, update.quest.currentProgress)
        assertEquals(QuestStatus.Completed, update.quest.status)
    }

    @Test
    fun updateQuestProgressDoesNotChangeWhenAlreadyLoggedToday() {
        val activeQuest = longTermQuest.copy(currentProgress = 2, status = QuestStatus.Active)

        val update = LongTermQuestProgress.updatedQuest(
            quest = activeQuest,
            progressDelta = 1,
            now = 10L,
            canUpdateToday = false
        )

        assertEquals(activeQuest, update.quest)
        assertFalse(update.completedNow)
        assertEquals(null, update.updatedAt)
    }

    @Test
    fun completedQuestDoesNotUpdateAgain() {
        val completedQuest = longTermQuest.copy(
            currentProgress = 5,
            status = QuestStatus.Completed
        )

        val update = LongTermQuestProgress.updatedQuest(completedQuest, progressDelta = 1, now = 10L)

        assertEquals(completedQuest, update.quest)
        assertFalse(update.completedNow)
    }

    @Test
    fun dailyQuestDoesNotUseLongTermProgressRules() {
        val dailyQuest = longTermQuest.copy(goalType = QuestGoalType.Daily)

        val update = LongTermQuestProgress.updatedQuest(dailyQuest, progressDelta = 1, now = 10L)

        assertEquals(dailyQuest, update.quest)
        assertFalse(update.completedNow)
    }
}
