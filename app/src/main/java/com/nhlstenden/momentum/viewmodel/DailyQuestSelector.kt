package com.nhlstenden.momentum.viewmodel

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.QuestStatus

/**
 * Pure, side-effect-free selection and ranking logic for the daily quest board.
 *
 * Kept separate from [QuestViewModel] so the "which quests should the user see and
 * in what order" rules have a single responsibility and can be unit-tested without
 * Firebase, coroutines, or Android. Everything here is a function of its inputs.
 */
object DailyQuestSelector {

    /**
     * The quests that should currently be visible, already filtered by status and
     * interests and sorted by recommendation strength. Mirrors the behaviour the UI
     * relied on before this logic was extracted.
     */
    fun visibleQuests(
        quests: List<Quest>,
        selectedStatus: QuestStatus?,
        selectedInterests: Set<String>,
        dailyQuestLimit: Int,
        dailyQuestIds: Set<String>,
        feedbackByQuestId: Map<String, QuestFeedbackType>,
        feedbackScoreByCategory: Map<com.nhlstenden.momentum.data.model.QuestCategory, Int>
    ): List<Quest> {
        val filteredQuests = selectedStatus?.let { status ->
            quests.filter { it.status == status }
        } ?: quests

        val interestFilteredQuests = if (selectedInterests.isEmpty()) {
            filteredQuests
        } else {
            filteredQuests.filter { it.category.label in selectedInterests }
        }.ifEmpty { filteredQuests }

        val visibleCandidates = if (selectedInterests.isNotEmpty() && selectedStatus == null) {
            interestFilteredQuests
                .filter { it.isLongTerm }
                .plus(
                    interestFilteredQuests
                        .filterNot { it.isLongTerm }
                        .takeBalancedDailyQuests(
                            selectedInterests = selectedInterests,
                            dailyQuestLimit = dailyQuestLimit,
                            feedbackByQuestId = feedbackByQuestId,
                            feedbackScoreByCategory = feedbackScoreByCategory
                        )
                )
        } else {
            interestFilteredQuests.dailyLimited(selectedStatus, dailyQuestIds)
        }

        return visibleCandidates.sortedWith(
            compareByDescending<Quest> {
                it.recommendationScore(feedbackByQuestId, feedbackScoreByCategory)
            }
                .thenBy { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )
    }

    /** The set of daily (non-long-term) quest ids that should be assigned for today. */
    fun dailyQuestIds(
        quests: List<Quest>,
        dailyQuestLimit: Int,
        feedbackByQuestId: Map<String, QuestFeedbackType>,
        feedbackScoreByCategory: Map<com.nhlstenden.momentum.data.model.QuestCategory, Int>
    ): Set<String> =
        quests
            .filterNot { it.isLongTerm }
            .prioritizedByFeedback(feedbackByQuestId, feedbackScoreByCategory)
            .take(dailyQuestLimit)
            .map { it.id }
            .toSet()

    private fun List<Quest>.dailyLimited(
        selectedStatus: QuestStatus?,
        dailyQuestIds: Set<String>
    ): List<Quest> {
        if (selectedStatus != null && selectedStatus != QuestStatus.Available) return this

        return filter { quest ->
            quest.isLongTerm || quest.status != QuestStatus.Available || quest.id in dailyQuestIds
        }
    }

    private fun List<Quest>.takeBalancedDailyQuests(
        selectedInterests: Set<String>,
        dailyQuestLimit: Int,
        feedbackByQuestId: Map<String, QuestFeedbackType>,
        feedbackScoreByCategory: Map<com.nhlstenden.momentum.data.model.QuestCategory, Int>
    ): List<Quest> {
        val prioritized = prioritizedByFeedback(feedbackByQuestId, feedbackScoreByCategory)
        if (selectedInterests.isEmpty()) return prioritized.take(dailyQuestLimit)

        val byInterest = selectedInterests
            .mapNotNull { interest ->
                val questsForInterest = prioritized.filter { it.category.label == interest }
                if (questsForInterest.isEmpty()) null else interest to questsForInterest
            }
            .toMap()

        val balanced = mutableListOf<Quest>()
        var index = 0
        while (balanced.size < dailyQuestLimit) {
            val nextRound = selectedInterests.mapNotNull { interest ->
                byInterest[interest]?.getOrNull(index)
            }
            if (nextRound.isEmpty()) break
            balanced += nextRound.filterNot { quest -> balanced.any { it.id == quest.id } }
            index += 1
        }

        return balanced
            .take(dailyQuestLimit)
            .ifEmpty { prioritized.take(dailyQuestLimit) }
    }

    private fun List<Quest>.prioritizedByFeedback(
        feedbackByQuestId: Map<String, QuestFeedbackType>,
        feedbackScoreByCategory: Map<com.nhlstenden.momentum.data.model.QuestCategory, Int>
    ): List<Quest> =
        sortedWith(
            compareByDescending<Quest> {
                it.recommendationScore(feedbackByQuestId, feedbackScoreByCategory)
            }
                .thenBy { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )

    private fun Quest.recommendationScore(
        feedbackByQuestId: Map<String, QuestFeedbackType>,
        feedbackScoreByCategory: Map<com.nhlstenden.momentum.data.model.QuestCategory, Int>
    ): Int =
        ((feedbackByQuestId[id]?.preferenceScore ?: 0) * DIRECT_FEEDBACK_WEIGHT) +
            (feedbackScoreByCategory[category] ?: 0)

    private const val DIRECT_FEEDBACK_WEIGHT = 100
}

/** How strongly a feedback reaction nudges recommendations, positive or negative. */
internal val QuestFeedbackType.preferenceScore: Int
    get() = when (this) {
        QuestFeedbackType.Like -> 2
        QuestFeedbackType.MoreLikeThis -> 1
        QuestFeedbackType.NotForMe -> -1
        QuestFeedbackType.Dislike -> -2
    }

/** Stable tie-break order for quest status when ranking the board. */
internal val QuestStatus.sortOrder: Int
    get() = when (this) {
        QuestStatus.Active -> 0
        QuestStatus.Available -> 1
        QuestStatus.Completed -> 2
        QuestStatus.Skipped -> 3
    }
