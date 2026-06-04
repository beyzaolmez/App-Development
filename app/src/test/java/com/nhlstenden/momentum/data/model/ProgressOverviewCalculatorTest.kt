package com.nhlstenden.momentum.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressOverviewCalculatorTest {
    @Test
    fun buildSummarizesCompletedQuestsAndActivityStats() {
        val overview = ProgressOverviewCalculator.build(
            quests = listOf(
                quest(id = "walk", title = "Take a walk", category = QuestCategory.Movement, status = QuestStatus.Completed),
                quest(id = "focus", title = "Focus block", category = QuestCategory.Focus, status = QuestStatus.Active),
                quest(id = "note", title = "Gratitude note", category = QuestCategory.Wellbeing, status = QuestStatus.Completed)
            ),
            progress = UserProgress(
                currentStreak = 4,
                completedQuestCount = 9,
                skippedQuestCount = 3,
                categoryCounts = mapOf(
                    "Movement" to 4,
                    "Wellbeing" to 5
                )
            ),
            dailyQuestLimit = 3
        )

        assertEquals(2, overview.completedToday)
        assertEquals(3, overview.totalDailyQuests)
        assertEquals(9, overview.totalCompletedQuests)
        assertEquals(12, overview.triedQuestCount)
        assertEquals(4, overview.currentStreak)
        assertEquals(2, overview.completedQuests.size)
        assertEquals("Take a walk", overview.completedQuests.first().title)
        assertEquals(2f / 3f, overview.dailyCompletionFraction, 0.001f)
        assertEquals(0.75f, overview.completionRateFraction, 0.001f)
    }

    @Test
    fun buildCreatesCategoryFractionsFromStoredProgress() {
        val overview = ProgressOverviewCalculator.build(
            quests = emptyList(),
            progress = UserProgress(
                completedQuestCount = 5,
                categoryCounts = mapOf(
                    "Wellbeing" to 3,
                    "Social" to 2,
                    "Unknown" to 8
                )
            ),
            dailyQuestLimit = 3
        )

        assertEquals(2, overview.categoryStats.size)
        assertTrue(overview.categoryStats.none { it.category.label == "Unknown" })
        assertEquals(2f / 5f, overview.categoryStats.first { it.category == QuestCategory.Social }.fraction, 0.001f)
        assertEquals(3f / 5f, overview.categoryStats.first { it.category == QuestCategory.Wellbeing }.fraction, 0.001f)
    }

    private fun quest(
        id: String,
        title: String,
        category: QuestCategory,
        status: QuestStatus
    ): Quest = Quest(
        id = id,
        title = title,
        description = "A small quest",
        category = category,
        xp = 10,
        difficulty = QuestDifficulty.Easy,
        estimatedMinutes = 10,
        steps = listOf("Try it"),
        status = status
    )
}
