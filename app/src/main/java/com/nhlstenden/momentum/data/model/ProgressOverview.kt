package com.nhlstenden.momentum.data.model

data class ProgressOverview(
    val completedToday: Int = 0,
    val totalDailyQuests: Int = 0,
    val totalCompletedQuests: Int = 0,
    val skippedQuestCount: Int = 0,
    val currentStreak: Int = 0,
    val categoryStats: List<CategoryProgressStat> = emptyList(),
    val completedQuests: List<CompletedQuestSummary> = emptyList(),
    val longTermQuests: List<LongTermQuestSummary> = emptyList()
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

data class LongTermQuestSummary(
    val id: String,
    val title: String,
    val category: QuestCategory,
    val currentProgress: Int,
    val targetProgress: Int,
    val progressUnit: String,
    val status: QuestStatus
) {
    val progressFraction: Float
        get() = safeFraction(currentProgress, targetProgress)
}

object ProgressOverviewCalculator {
    fun build(
        quests: List<Quest>,
        progress: UserProgress,
        dailyQuestLimit: Int
    ): ProgressOverview {
        val dailyQuests = quests.filterNot { it.isLongTerm }
        val completedQuests = dailyQuests
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

        val longTermQuests = quests
            .filter { it.isLongTerm && it.status != QuestStatus.Skipped }
            .sortedWith(compareBy<Quest> { it.status.sortOrder }.thenBy { it.title })
            .map { quest ->
                LongTermQuestSummary(
                    id = quest.id,
                    title = quest.title,
                    category = quest.category,
                    currentProgress = quest.currentProgress,
                    targetProgress = quest.targetProgress,
                    progressUnit = quest.progressUnit,
                    status = quest.status
                )
            }

        return ProgressOverview(
            completedToday = minOf(completedQuests.size, dailyQuestLimit),
            totalDailyQuests = minOf(dailyQuestLimit, dailyQuests.size),
            totalCompletedQuests = progress.completedQuestCount,
            skippedQuestCount = progress.skippedQuestCount,
            currentStreak = progress.currentStreak,
            categoryStats = categoryStats,
            completedQuests = completedQuests,
            longTermQuests = longTermQuests
        )
    }
}

private val QuestStatus.sortOrder: Int
    get() = when (this) {
        QuestStatus.Active -> 0
        QuestStatus.Available -> 1
        QuestStatus.Completed -> 2
        QuestStatus.Skipped -> 3
    }

private fun safeFraction(value: Int, total: Int): Float =
    if (total <= 0) 0f else value.toFloat() / total.toFloat()
