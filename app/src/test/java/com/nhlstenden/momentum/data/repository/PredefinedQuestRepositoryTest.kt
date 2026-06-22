package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.LongTermQuestProgress
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestGoalType
import com.nhlstenden.momentum.data.model.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredefinedQuestRepositoryTest {

    private val repository = PredefinedQuestRepository()

    @Test
    fun predefinedQuestsIncludeWorkoutContent() {
        val workoutQuestIds = setOf(
            "workout-session",
            "bodyweight-circuit",
            "cardio-intervals",
            "core-reset",
            "workout-streak"
        )
        val workoutQuests = repository.getQuests().filter { it.id in workoutQuestIds }

        assertEquals(workoutQuestIds.size, workoutQuests.size)
        assertTrue(workoutQuests.any { it.id == "bodyweight-circuit" })
        assertTrue(workoutQuests.any { it.id == "cardio-intervals" })
        assertTrue(workoutQuests.any { it.id == "core-reset" })
        assertTrue(workoutQuests.any { it.id == "workout-streak" })
    }

    @Test
    fun workoutQuestsAppearInMovementCategory() {
        val workoutQuestIds = setOf(
            "workout-session",
            "bodyweight-circuit",
            "cardio-intervals",
            "core-reset",
            "workout-streak"
        )

        val workoutQuests = repository.getQuests().filter { it.id in workoutQuestIds }

        assertEquals(workoutQuestIds.size, workoutQuests.size)
        assertTrue(workoutQuests.all { it.category == QuestCategory.Movement })
    }

    @Test
    fun dailyWorkoutQuestsCanBeCompletedManually() {
        val dailyWorkoutQuests = repository.getQuests()
            .filter { it.category == QuestCategory.Movement && it.id != "workout-streak" }
            .filter { it.goalType == QuestGoalType.Daily }

        assertFalse(dailyWorkoutQuests.isEmpty())
        dailyWorkoutQuests.forEach { quest ->
            val completedQuest = quest.copy(status = QuestStatus.Completed)

            assertEquals(QuestStatus.Completed, completedQuest.status)
            assertTrue(quest.steps.isNotEmpty())
            assertTrue(quest.xp > 0)
            assertTrue(quest.targetProgress >= 1)
        }
    }

    @Test
    fun longTermWorkoutQuestCanBeProgressedToCompletion() {
        val workoutStreak = repository.getQuestById("workout-streak")

        assertNotNull(workoutStreak)
        assertEquals(QuestGoalType.LongTerm, workoutStreak!!.goalType)
        assertEquals(4, workoutStreak.targetProgress)
        assertEquals("workouts", workoutStreak.progressUnit)

        val almostComplete = workoutStreak.copy(
            status = QuestStatus.Active,
            currentProgress = 3
        )
        val update = LongTermQuestProgress.updatedQuest(
            quest = almostComplete,
            progressDelta = 1,
            now = 10L
        )

        assertEquals(4, update.quest.currentProgress)
        assertEquals(QuestStatus.Completed, update.quest.status)
        assertTrue(update.completedNow)
    }
}
