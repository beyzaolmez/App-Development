package com.nhlstenden.momentum.data.model

data class ProgressOverview(
    val completedToday: Int = 0,
    val totalDailyQuests: Int = 0,
    val totalCompletedQuests: Int = 0,
    val skippedQuestCount: Int = 0,
    val currentStreak: Int = 0,
    val categoryStats: List<CategoryProgressStat> = emptyList(),
    val completedQuests: List<CompletedQuestSummary> = emptyList()
) {
    val dailyCompletionFraction: Float
        get() = safeFraction(completedToday, totalDailyQuests)

    val triedQuestCount: Int
        get() = totalCompletedQuests + skippedQuestCount

    val completionRateFraction: Float
        get() = safeFraction(totalCompletedQuests, triedQuestCount)
}

data class CategoryProgressStat(
    val category: QuestCategory,
    val completedCount: Int,
    val fraction: Float
)

data class CompletedQuestSummary(
    val id: String,
    val title: String,
    val category: QuestCategory,
    val estimatedMinutes: Int
)

object ProgressOverviewCalculator {
    fun build(
        quests: List<Quest>,
        progress: UserProgress,
        dailyQuestLimit: Int
    ): ProgressOverview {
        val completedQuests = quests
            .filter { it.status == QuestStatus.Completed }
            .sortedWith(compareBy<Quest> { it.category.label }.thenBy { it.title })
            .map { quest ->
                CompletedQuestSummary(
                    id = quest.id,
                    title = quest.title,
                    category = quest.category,
                    estimatedMinutes = quest.estimatedMinutes
                )
            }

        val categoryStats = progress.categoryCounts
            .mapNotNull { (categoryName, count) ->
                val category = QuestCategory.entries
                    .firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
                    ?: return@mapNotNull null
                category to count
            }
            .sortedBy { it.first.label }
            .let { counts ->
                val total = counts.sumOf { it.second }.coerceAtLeast(1)
                counts.map { (category, count) ->
                    CategoryProgressStat(
                        category = category,
                        completedCount = count,
                        fraction = safeFraction(count, total)
                    )
                }
            }

        return ProgressOverview(
            completedToday = minOf(completedQuests.size, dailyQuestLimit),
            totalDailyQuests = minOf(dailyQuestLimit, quests.size),
            totalCompletedQuests = progress.completedQuestCount,
            skippedQuestCount = progress.skippedQuestCount,
            currentStreak = progress.currentStreak,
            categoryStats = categoryStats,
            completedQuests = completedQuests
        )
    }
}

private fun safeFraction(value: Int, total: Int): Float =
    if (total <= 0) 0f else value.toFloat() / total.toFloat()
