package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.QuestFeedback

interface QuestFeedbackRepository {
    suspend fun getFeedback(uid: String): List<QuestFeedback>
    suspend fun saveFeedback(uid: String, feedback: QuestFeedback)
}

class InMemoryQuestFeedbackRepository : QuestFeedbackRepository {
    private val feedbackByUser = mutableMapOf<String, List<QuestFeedback>>()

    override suspend fun getFeedback(uid: String): List<QuestFeedback> =
        feedbackByUser[uid].orEmpty().sortedByDescending { it.createdAt }

    override suspend fun saveFeedback(uid: String, feedback: QuestFeedback) {
        feedbackByUser[uid] = feedbackByUser[uid].orEmpty() + feedback
    }
}
