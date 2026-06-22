package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nhlstenden.momentum.data.model.QuestFeedback
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.QuestCategory
import kotlinx.coroutines.tasks.await

interface QuestFeedbackRepository {
    suspend fun getFeedback(uid: String): List<QuestFeedback>
    suspend fun saveFeedback(uid: String, feedback: QuestFeedback)
}

class InMemoryQuestFeedbackRepository : QuestFeedbackRepository {
    private val lock = Any()
    private val feedbackByUser = mutableMapOf<String, List<QuestFeedback>>()

    override suspend fun getFeedback(uid: String): List<QuestFeedback> =
        synchronized(lock) { feedbackByUser[uid].orEmpty().sortedByDescending { it.createdAt } }

    override suspend fun saveFeedback(uid: String, feedback: QuestFeedback) = synchronized(lock) {
        feedbackByUser[uid] = feedbackByUser[uid].orEmpty() + feedback
    }
}

class FirestoreQuestFeedbackRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : QuestFeedbackRepository {
    override suspend fun getFeedback(uid: String): List<QuestFeedback> {
        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("questFeedback")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            val questId = document.getString("questId") ?: return@mapNotNull null
            QuestFeedback(
                feedbackId = document.getString("feedbackId") ?: document.id,
                questId = questId,
                category = document.getString("category").toQuestCategory(),
                feedbackType = document.getString("feedbackType").toQuestFeedbackType(),
                createdAt = document.getLong("createdAt") ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun saveFeedback(uid: String, feedback: QuestFeedback) {
        firestore
            .collection("users")
            .document(uid)
            .collection("questFeedback")
            .document(feedback.feedbackId)
            .set(
                mapOf(
                    "feedbackId" to feedback.feedbackId,
                    "questId" to feedback.questId,
                    "category" to feedback.category.name,
                    "feedbackType" to feedback.feedbackType.name,
                    "createdAt" to feedback.createdAt
                )
            )
            .await()
    }
}

private fun String?.toQuestCategory(): QuestCategory =
    enumValues<QuestCategory>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: QuestCategory.Wellbeing

private fun String?.toQuestFeedbackType(): QuestFeedbackType =
    enumValues<QuestFeedbackType>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: QuestFeedbackType.Like
