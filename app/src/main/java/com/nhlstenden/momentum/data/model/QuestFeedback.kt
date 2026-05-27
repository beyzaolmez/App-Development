package com.nhlstenden.momentum.data.model

data class QuestFeedback(
    val feedbackId: String,
    val questId: String,
    val category: QuestCategory,
    val feedbackType: QuestFeedbackType,
    val createdAt: Long
)

enum class QuestFeedbackType(val label: String) {
    Like("Like quest"),
    Dislike("Dislike quest"),
    MoreLikeThis("More like this"),
    NotForMe("Not for me")
}
