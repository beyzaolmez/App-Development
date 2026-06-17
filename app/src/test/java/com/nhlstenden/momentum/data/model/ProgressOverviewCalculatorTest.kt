package com.nhlstenden.momentum.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressOverviewCalculatorTest {

    @Test
    fun buildSeparatesDailyAndLongTermProgress() {
        val dailyQuest = quest(
            id = "daily",
            status = QuestStatus.Completed,
            goalType = QuestGoalType.Daily
        )
        val longTermQuest = quest(
            id = "long-term",
            status = QuestStatus.Active,
            goalType = QuestGoalType.LongTerm,
            currentProgress = 2,
            targetProgress = 5,
            progressUnit = "sessions"
        )

        val overview = ProgressOverviewCalculator.build(
            quests = listOf(dailyQuest, longTermQuest),
            progress = UserProgress(completedQuestCount = 1),
            dailyQuestLimit = 3
        )

        assertEquals(1, overview.completedToday)
        assertEquals(1, overview.totalDailyQuests)
        assertEquals(1, overview.longTermQuests.size)
        assertEquals(2, overview.longTermQuests.first().currentProgress)
        assertEquals(5, overview.longTermQuests.first().targetProgress)
    }

    private fun quest(
        id: String,
        status: QuestStatus,
        goalType: QuestGoalType,
        currentProgress: Int = 0,
        targetProgress: Int = 1,
        progressUnit: String = "completion"
    ): Quest = Quest(
        id = id,
        title = id,
        description = id,
        category = QuestCategory.Focus,
        xp = 100,
        difficulty = QuestDifficulty.Easy,
        estimatedMinutes = 10,
        steps = emptyList(),
        status = status,
        goalType = goalType,
        currentProgress = currentProgress,
        targetProgress = targetProgress,
        progressUnit = progressUnit
    )
}
